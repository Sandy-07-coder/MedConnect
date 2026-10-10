package com.mediconnect.forecast_service.model;

import java.time.LocalDate;

/**
 * Signals that a specific batch is at risk of expiring before
 * the facility can consume it at the forecast rate.
 */
public record ExpiryAlert(
        Long batchId,
        String batchNumber,
        int batchQuantity,
        LocalDate expiryDate,
        int daysUntilExpiry,
        int estimatedQuantityAtRisk,
        String explanation) {}
