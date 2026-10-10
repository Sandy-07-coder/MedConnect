package com.mediconnect.forecast_service.dto;

/**
 * Mirrors the InventorySummaryDto returned by the Inventory Service.
 * The Forecast Service deserialises the JSON response into this record.
 */
public record InventorySummaryDto(
        Long facilityId,
        String facilityName,
        String district,
        Double latitude,
        Double longitude,
        Long medicineId,
        String medicineName,
        String unitOfMeasurement,
        int totalUsableQuantity) {}
