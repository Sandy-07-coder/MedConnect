package com.mediconnect.forecast_service.model;

/**
 * A proposed transfer of stock from a donor facility to a receiving facility.
 * This is a RECOMMENDATION only — it does not mutate any stock quantities.
 * A human operator must approve the transfer before any physical movement occurs.
 */
public record RedistributionRecommendation(
        Long donorFacilityId,
        String donorFacilityName,
        int donorCurrentStock,
        double donorForecastDailyDemand,
        int donorSafetyStockTarget,
        int donorTransferableQuantity,
        int recommendedTransferQuantity,
        double estimatedDistanceKm,
        double receiverProjectedCoverageDaysAfterTransfer,
        String explanation) {}
