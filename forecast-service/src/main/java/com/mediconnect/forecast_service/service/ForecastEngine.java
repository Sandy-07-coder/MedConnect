package com.mediconnect.forecast_service.service;

import com.mediconnect.forecast_service.dto.ConsumptionRecordDto;
import com.mediconnect.forecast_service.dto.StockBatchDto;
import com.mediconnect.forecast_service.model.ExpiryAlert;
import com.mediconnect.forecast_service.model.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculation logic — no HTTP calls, no database access.
 * Inputs are plain Java objects already fetched by the AnalysisService.
 * This makes this class easy to unit-test in isolation.
 *
 * DEMAND FORECAST:
 *   Phase A — Moving average: 7-day and 30-day averages computed separately.
 *   The forecast uses the 7-day average when it differs significantly from the
 *   30-day average (short-term spike detection), otherwise uses the 30-day.
 *   Linear regression is possible with more data but not needed for PoC.
 *
 * DAYS-TO-STOCK-OUT:
 *   = usable stock / forecast daily demand
 *   Zero-stock → 0 days. Near-zero demand → Large/Infinite days (no risk).
 *
 * RISK CLASSIFICATION:
 *   Thresholds are PoC prototypes, not official supply-chain standards.
 *   They are externalised to application.properties for easy adjustment.
 */
@Slf4j
@Service
public class ForecastEngine {

    @Value("${forecast.risk.critical-days}")
    private int criticalDays;

    @Value("${forecast.risk.high-days}")
    private int highDays;

    @Value("${forecast.risk.medium-days}")
    private int mediumDays;

    private static final double NEAR_ZERO_DEMAND_THRESHOLD = 0.1;

    // ─── Demand Forecasting ───────────────────────────────────────────────────

    /**
     * Calculates average daily consumption over the last N days from records.
     * Returns 0.0 if no records exist (missing history is treated as zero demand
     * with a warning — callers should flag this as uncertain).
     */
    public double averageDailyConsumption(List<ConsumptionRecordDto> records, int days) {
        if (records == null || records.isEmpty()) return 0.0;
        LocalDate cutoff = LocalDate.now().minusDays(days);
        return records.stream()
                .filter(r -> !r.consumptionDate().isBefore(cutoff))
                .mapToInt(ConsumptionRecordDto::quantityConsumed)
                .average()
                .orElse(0.0);
    }

    /**
     * Selects the best forecast:
     * - If 7-day average is more than 20% higher than 30-day, demand is rising — use 7-day.
     * - If 7-day average is more than 20% lower than 30-day, demand is falling — use 7-day.
     * - Otherwise use 30-day average (more stable, less noise).
     *
     * This is a simple heuristic, not a trained model. It communicates uncertainty
     * by flagging when the two averages diverge significantly.
     */
    public double selectForecastDemand(double sevenDayAvg, double thirtyDayAvg) {
        if (thirtyDayAvg < NEAR_ZERO_DEMAND_THRESHOLD) return sevenDayAvg;
        double ratio = sevenDayAvg / thirtyDayAvg;
        // If recent trend deviates >20%, use the more recent signal
        if (ratio > 1.20 || ratio < 0.80) return sevenDayAvg;
        return thirtyDayAvg;
    }

    // ─── Days-to-Stock-Out ────────────────────────────────────────────────────

    /**
     * Estimates how many days the current usable stock will last.
     *
     * Important caveats (these are estimates, not guarantees):
     * - Assumes constant daily demand equal to the forecast average.
     * - Does not account for supply deliveries that may arrive before stock-out.
     * - Does not account for demand spikes (disease outbreaks, etc.).
     * - Returns Double.MAX_VALUE when demand is near zero (no risk).
     * - Returns 0.0 when stock is already zero.
     */
    public double daysToStockOut(int usableStock, double forecastDailyDemand) {
        if (usableStock <= 0) return 0.0;
        if (forecastDailyDemand < NEAR_ZERO_DEMAND_THRESHOLD) return Double.MAX_VALUE;
        return (double) usableStock / forecastDailyDemand;
    }

    // ─── Risk Classification ──────────────────────────────────────────────────

    public RiskLevel classifyRisk(double daysToStockOut) {
        if (daysToStockOut == Double.MAX_VALUE) return RiskLevel.LOW;
        if (daysToStockOut <= criticalDays)    return RiskLevel.CRITICAL;
        if (daysToStockOut <= highDays)        return RiskLevel.HIGH;
        if (daysToStockOut <= mediumDays)      return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    // ─── Expiry Risk ──────────────────────────────────────────────────────────

    /**
     * For each non-expired batch, checks whether it is likely to expire before
     * the facility can consume it at the forecast demand rate.
     *
     * Uses First-Expiry-First-Out (FEFO) allocation:
     * Stock consumed is deducted from the earliest-expiring batches first.
     * A later batch only starts being consumed once earlier batches are exhausted.
     *
     * A batch is flagged if its quantity will not be fully consumed before expiry.
     */
    public List<ExpiryAlert> detectExpiryRisk(List<StockBatchDto> batches,
                                               double forecastDailyDemand) {
        List<ExpiryAlert> alerts = new ArrayList<>();
        if (forecastDailyDemand < NEAR_ZERO_DEMAND_THRESHOLD) {
            // If demand is near zero, every batch is at expiry risk
            for (StockBatchDto batch : batches) {
                if (!batch.expired() && batch.quantity() > 0) {
                    alerts.add(buildExpiryAlert(batch, batch.quantity(),
                            "Demand is near zero — entire batch at risk of expiry before use."));
                }
            }
            return alerts;
        }

        // Sort batches by expiry date (FEFO order)
        List<StockBatchDto> sorted = batches.stream()
                .filter(b -> !b.expired() && b.quantity() > 0)
                .sorted((a, b) -> a.expiryDate().compareTo(b.expiryDate()))
                .toList();

        double remainingDailyDemand = forecastDailyDemand;
        double cumulativeConsumed = 0;

        for (StockBatchDto batch : sorted) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), batch.expiryDate());
            double consumableFromThisBatch = remainingDailyDemand * daysUntilExpiry - cumulativeConsumed;

            if (consumableFromThisBatch < batch.quantity()) {
                int atRisk = batch.quantity() - (int) Math.max(0, consumableFromThisBatch);
                String explanation = String.format(
                        "Batch %s has %d units but only ~%.0f units can be consumed before expiry on %s " +
                        "at forecast demand of %.1f/day. Approximately %d units at risk.",
                        batch.batchNumber(), batch.quantity(), Math.max(0, consumableFromThisBatch),
                        batch.expiryDate(), forecastDailyDemand, atRisk);
                alerts.add(buildExpiryAlert(batch, atRisk, explanation));
            }
            cumulativeConsumed += batch.quantity();
        }
        return alerts;
    }

    private ExpiryAlert buildExpiryAlert(StockBatchDto batch, int atRisk, String explanation) {
        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), batch.expiryDate());
        return new ExpiryAlert(batch.id(), batch.batchNumber(), batch.quantity(),
                batch.expiryDate(), (int) daysUntilExpiry, atRisk, explanation);
    }

    // ─── Analysis Explanation ─────────────────────────────────────────────────

    public String buildExplanation(String facilityName, String medicineName,
                                    int usableStock, double sevenDay, double thirtyDay,
                                    double forecast, double daysToStockOut, RiskLevel risk) {
        String demandNote = "";
        if (thirtyDay > NEAR_ZERO_DEMAND_THRESHOLD) {
            double ratio = sevenDay / thirtyDay;
            if (ratio > 1.20) demandNote = " Recent 7-day demand is rising (+%.0f%% vs 30-day average).".formatted((ratio - 1) * 100);
            else if (ratio < 0.80) demandNote = " Recent 7-day demand is falling (%.0f%% of 30-day average).".formatted(ratio * 100);
        }

        String daysStr = daysToStockOut == Double.MAX_VALUE
                ? "indefinite (demand near zero)"
                : "%.1f days".formatted(daysToStockOut);

        return "%s holds %d usable units of %s. 7-day avg demand: %.1f/day, 30-day avg: %.1f/day. "
                .formatted(facilityName, usableStock, medicineName, sevenDay, thirtyDay)
               + "Forecast demand used: %.1f/day.%s ".formatted(forecast, demandNote)
               + "Estimated days to stock-out: %s. Risk level: %s.".formatted(daysStr, risk);
    }
}
