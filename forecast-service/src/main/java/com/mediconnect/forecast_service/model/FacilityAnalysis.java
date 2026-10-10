package com.mediconnect.forecast_service.model;

import java.util.List;

/**
 * Complete analysis result for one facility+medicine pair.
 * This is what the /api/analysis endpoint returns per row.
 */
public record FacilityAnalysis(
        Long facilityId,
        String facilityName,
        String district,
        Long medicineId,
        String medicineName,
        String unitOfMeasurement,
        int usableStock,
        double forecastDailyDemand,
        double sevenDayAvgDemand,
        double thirtyDayAvgDemand,
        double daysToStockOut,
        RiskLevel riskLevel,
        List<ExpiryAlert> expiryAlerts,
        RedistributionRecommendation redistribution,
        String analysisExplanation) {}
