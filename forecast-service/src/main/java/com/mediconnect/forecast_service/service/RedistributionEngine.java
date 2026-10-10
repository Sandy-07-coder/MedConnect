package com.mediconnect.forecast_service.service;

import com.mediconnect.forecast_service.dto.InventorySummaryDto;
import com.mediconnect.forecast_service.model.RedistributionRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic redistribution algorithm.
 *
 * For each facility facing a shortage, this engine:
 *  1. Computes receiver's target stock and deficit.
 *  2. Scans all other facilities with the same medicine for surplus.
 *  3. Calculates how much a donor can safely give without dropping below
 *     its own safety-stock target.
 *  4. Ranks donors by (surplus descending, distance ascending).
 *  5. Returns the best feasible recommendation, or null if none exists.
 *
 * KEY SAFETY RULE: A recommendation is rejected if the transfer would reduce
 * the donor's projected stock below its own safety-stock threshold.
 * We do not put donors at risk to help receivers.
 *
 * This is a recommendation only — it does NOT update any stock quantities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedistributionEngine {

    private final ForecastEngine forecastEngine;

    @Value("${forecast.redistribution.target-coverage-days}")
    private int targetCoverageDays;         // e.g. 14 days

    @Value("${forecast.redistribution.safety-stock-days}")
    private int safetyStockDays;            // e.g. 7 days — donor must keep this much

    private static final int TRANSFER_PACK_SIZE = 100; // round transfers to nearest 100 units

    /**
     * Find the best donor for a receiving facility.
     *
     * @param receiver       The inventory row for the receiving facility+medicine
     * @param receiverDemand Forecast daily demand for the receiver
     * @param allInventory   All inventory rows for the same medicine (across all facilities)
     * @param allDemands     Parallel list of forecast demands for each inventory row
     */
    public Optional<RedistributionRecommendation> findBestDonor(
            InventorySummaryDto receiver,
            double receiverDemand,
            List<InventorySummaryDto> allInventory,
            List<Double> allDemands) {

        // Step 1: Calculate receiver's target stock and deficit
        int receiverTarget = (int) Math.ceil(receiverDemand * targetCoverageDays);
        int receiverDeficit = Math.max(0, receiverTarget - receiver.totalUsableQuantity());

        if (receiverDeficit <= 0) {
            log.debug("{} has no deficit — no redistribution needed.", receiver.facilityName());
            return Optional.empty();
        }

        // Step 2: Evaluate all potential donors
        record DonorCandidate(InventorySummaryDto inventory, double demand,
                              int transferable, double distanceKm) {}

        List<DonorCandidate> candidates = new java.util.ArrayList<>();

        for (int i = 0; i < allInventory.size(); i++) {
            InventorySummaryDto donor = allInventory.get(i);
            double donorDemand = allDemands.get(i);

            // Cannot donate to yourself
            if (donor.facilityId().equals(receiver.facilityId())) continue;

            // Step 3: Donor's safety stock target (must retain at least this much)
            int donorSafetyStock = (int) Math.ceil(donorDemand * safetyStockDays);

            // Step 4: How much can the donor transfer without going below safety stock?
            int donorTransferable = donor.totalUsableQuantity() - donorSafetyStock;

            // Safety rule: reject if donor cannot spare anything
            if (donorTransferable <= 0) {
                log.debug("{} cannot donate — transferable stock would be {}.",
                        donor.facilityName(), donorTransferable);
                continue;
            }

            double distKm = haversineKm(
                    receiver.latitude(), receiver.longitude(),
                    donor.latitude(), donor.longitude());

            candidates.add(new DonorCandidate(donor, donorDemand, donorTransferable, distKm));
        }

        if (candidates.isEmpty()) return Optional.empty();

        // Step 5: Rank donors — most transferable first, then closest
        candidates.sort(Comparator
                .comparingInt(DonorCandidate::transferable).reversed()
                .thenComparingDouble(DonorCandidate::distanceKm));

        DonorCandidate best = candidates.get(0);

        // Step 6: Determine actual transfer quantity
        int rawTransfer = Math.min(receiverDeficit, best.transferable());
        // Round down to nearest pack size (ensures physically meaningful quantity)
        int transferQty = (rawTransfer / TRANSFER_PACK_SIZE) * TRANSFER_PACK_SIZE;
        if (transferQty == 0 && rawTransfer > 0) transferQty = rawTransfer; // below one pack — transfer as-is

        // Step 7: Project receiver coverage after transfer
        double receiverStockAfter = receiver.totalUsableQuantity() + transferQty;
        double coverageAfter = receiverDemand > 0.1
                ? receiverStockAfter / receiverDemand
                : Double.MAX_VALUE;

        int donorSafetyStock = (int) Math.ceil(best.demand() * safetyStockDays);

        String explanation = buildRedistributionExplanation(
                receiver, receiverDemand, receiverTarget, receiverDeficit,
                best.inventory(), best.demand(), donorSafetyStock,
                best.transferable(), transferQty, coverageAfter, best.distanceKm());

        return Optional.of(new RedistributionRecommendation(
                best.inventory().facilityId(),
                best.inventory().facilityName(),
                best.inventory().totalUsableQuantity(),
                best.demand(),
                donorSafetyStock,
                best.transferable(),
                transferQty,
                best.distanceKm(),
                coverageAfter,
                explanation));
    }

    /**
     * Haversine formula — straight-line distance between two lat/lon points in km.
     * Used for ranking donors. Does not account for road distance or travel time.
     */
    private double haversineKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return Double.MAX_VALUE;
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String buildRedistributionExplanation(
            InventorySummaryDto receiver, double receiverDemand,
            int receiverTarget, int receiverDeficit,
            InventorySummaryDto donor, double donorDemand,
            int donorSafetyStock, int donorTransferable,
            int transferQty, double coverageAfter, double distanceKm) {

        String coverageStr = coverageAfter == Double.MAX_VALUE
                ? "indefinite" : "%.1f days".formatted(coverageAfter);

        return ("Receiver %s needs %d units (target for %d-day coverage) but holds only %d (deficit: %d). " +
                "Donor %s holds %d units at %.1f/day demand; safety stock target is %d units, " +
                "leaving %d transferable. Recommending transfer of %d units (%.1f km away). " +
                "Receiver projected coverage after transfer: %s.")
                .formatted(
                        receiver.facilityName(), receiverTarget, targetCoverageDays,
                        receiver.totalUsableQuantity(), receiverDeficit,
                        donor.facilityName(), donor.totalUsableQuantity(), donorDemand,
                        donorSafetyStock, donorTransferable, transferQty, distanceKm, coverageStr);
    }
}
