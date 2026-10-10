package com.mediconnect.forecast_service.service;

import com.mediconnect.forecast_service.client.InventoryClient;
import com.mediconnect.forecast_service.dto.ConsumptionRecordDto;
import com.mediconnect.forecast_service.dto.InventorySummaryDto;
import com.mediconnect.forecast_service.dto.StockBatchDto;
import com.mediconnect.forecast_service.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestration layer — coordinates data fetching and delegates calculations.
 *
 * Flow:
 *  1. Fetch all inventory rows from the Inventory Service (filtered by district/medicine).
 *  2. For each facility+medicine row, fetch consumption history.
 *  3. Compute 7-day and 30-day demand averages via ForecastEngine.
 *  4. Compute days-to-stock-out and risk level.
 *  5. Detect expiry risks.
 *  6. Run redistribution engine for facilities with HIGH or CRITICAL risk.
 *  7. Return a list of FacilityAnalysis records.
 *
 * Inventory Service unavailability: if the client returns an empty list,
 * this service throws 503 rather than silently returning empty analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final InventoryClient inventoryClient;
    private final ForecastEngine forecastEngine;
    private final RedistributionEngine redistributionEngine;

    public List<FacilityAnalysis> analyse(String district, Long medicineId, int forecastHorizonDays) {

        // Step 1: Fetch inventory
        List<InventorySummaryDto> inventory = inventoryClient.getInventory(district, null, medicineId);

        if (inventory.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No inventory data returned from Inventory Service. " +
                    "The service may be unreachable or no matching records exist.");
        }

        // Step 2: Pre-compute forecast demands for all rows (needed for redistribution context)
        Map<Long, Double> demandByFacilityMedicine = new HashMap<>();
        String endDate = LocalDate.now().toString();
        String startDate30 = LocalDate.now().minusDays(30).toString();

        for (InventorySummaryDto row : inventory) {
            List<ConsumptionRecordDto> history = inventoryClient.getConsumption(
                    row.facilityId(), row.medicineId(), startDate30, endDate);
            double sevenDay  = forecastEngine.averageDailyConsumption(history, 7);
            double thirtyDay = forecastEngine.averageDailyConsumption(history, 30);
            double forecast  = forecastEngine.selectForecastDemand(sevenDay, thirtyDay);
            // Key: facilityId * 100000 + medicineId (simple compound key for PoC)
            demandByFacilityMedicine.put(compoundKey(row.facilityId(), row.medicineId()), forecast);
        }

        // Step 3: Build full analysis per row
        List<FacilityAnalysis> results = new ArrayList<>();

        for (InventorySummaryDto row : inventory) {
            long key = compoundKey(row.facilityId(), row.medicineId());
            double forecast = demandByFacilityMedicine.getOrDefault(key, 0.0);

            // Re-fetch consumption to get 7-day and 30-day separately for display
            List<ConsumptionRecordDto> history = inventoryClient.getConsumption(
                    row.facilityId(), row.medicineId(), startDate30, endDate);
            double sevenDay  = forecastEngine.averageDailyConsumption(history, 7);
            double thirtyDay = forecastEngine.averageDailyConsumption(history, 30);

            double dts = forecastEngine.daysToStockOut(row.totalUsableQuantity(), forecast);
            RiskLevel risk = forecastEngine.classifyRisk(dts);

            // Expiry risk
            List<StockBatchDto> batches = inventoryClient.getBatches(row.facilityId(), row.medicineId());
            List<ExpiryAlert> expiryAlerts = forecastEngine.detectExpiryRisk(batches, forecast);

            // Redistribution (only for HIGH or CRITICAL risk — no point finding donors for LOW risk)
            RedistributionRecommendation redistribution = null;
            if (risk == RiskLevel.CRITICAL || risk == RiskLevel.HIGH) {
                // Gather all facilities' inventory for the same medicine (donor candidates)
                List<InventorySummaryDto> sameMedicine = inventory.stream()
                        .filter(r -> r.medicineId().equals(row.medicineId()))
                        .collect(Collectors.toList());
                List<Double> sameMedicineDemands = sameMedicine.stream()
                        .map(r -> demandByFacilityMedicine.getOrDefault(
                                compoundKey(r.facilityId(), r.medicineId()), 0.0))
                        .collect(Collectors.toList());

                redistribution = redistributionEngine
                        .findBestDonor(row, forecast, sameMedicine, sameMedicineDemands)
                        .orElse(null);
            }

            String explanation = forecastEngine.buildExplanation(
                    row.facilityName(), row.medicineName(),
                    row.totalUsableQuantity(), sevenDay, thirtyDay,
                    forecast, dts, risk);

            results.add(new FacilityAnalysis(
                    row.facilityId(), row.facilityName(), row.district(),
                    row.medicineId(), row.medicineName(), row.unitOfMeasurement(),
                    row.totalUsableQuantity(), forecast, sevenDay, thirtyDay,
                    dts, risk, expiryAlerts, redistribution, explanation));
        }

        // Sort: CRITICAL first, then HIGH, MEDIUM, LOW
        results.sort(Comparator.comparingInt(a -> a.riskLevel().ordinal()));
        return results;
    }

    /** Dashboard summary statistics */
    public Map<String, Object> getDashboardSummary() {
        List<InventorySummaryDto> inventory = inventoryClient.getInventory(null, null, null);
        List<StockBatchDto> expiringBatches = inventoryClient.getBatchesExpiringWithin(30);

        if (inventory.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Inventory Service is unreachable.");
        }

        String endDate   = LocalDate.now().toString();
        String startDate = LocalDate.now().minusDays(30).toString();

        long atRiskFacilities = 0;
        long redistributionRecommendations = 0;

        // Quick pass to count risk levels (reuses same logic as full analysis)
        Map<Long, Double> demandCache = new HashMap<>();
        for (InventorySummaryDto row : inventory) {
            if (!demandCache.containsKey(compoundKey(row.facilityId(), row.medicineId()))) {
                List<ConsumptionRecordDto> h = inventoryClient.getConsumption(
                        row.facilityId(), row.medicineId(), startDate, endDate);
                double s7  = forecastEngine.averageDailyConsumption(h, 7);
                double s30 = forecastEngine.averageDailyConsumption(h, 30);
                demandCache.put(compoundKey(row.facilityId(), row.medicineId()),
                        forecastEngine.selectForecastDemand(s7, s30));
            }
        }

        Set<Long> atRiskFacilityIds = new HashSet<>();
        for (InventorySummaryDto row : inventory) {
            double demand = demandCache.getOrDefault(compoundKey(row.facilityId(), row.medicineId()), 0.0);
            double dts = forecastEngine.daysToStockOut(row.totalUsableQuantity(), demand);
            RiskLevel risk = forecastEngine.classifyRisk(dts);
            if (risk == RiskLevel.CRITICAL || risk == RiskLevel.HIGH) {
                atRiskFacilityIds.add(row.facilityId());
                redistributionRecommendations++;
            }
        }

        // Count unique facilities monitored
        long facilitiesMonitored = inventory.stream()
                .map(InventorySummaryDto::facilityId)
                .distinct().count();

        return Map.of(
                "facilitiesMonitored",              facilitiesMonitored,
                "facilitiesAtRisk",                 atRiskFacilityIds.size(),
                "batchesWithExpiryRisk",            expiringBatches.size(),
                "pendingRedistributionRecommendations", redistributionRecommendations,
                "generatedAt",                      LocalDate.now().toString()
        );
    }

    private long compoundKey(Long facilityId, Long medicineId) {
        return facilityId * 100_000L + medicineId;
    }
}
