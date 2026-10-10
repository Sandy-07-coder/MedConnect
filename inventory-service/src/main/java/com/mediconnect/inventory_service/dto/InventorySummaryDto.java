package com.mediconnect.inventory_service.dto;

/**
 * Aggregated usable (non-expired) stock for a facility+medicine pair.
 * The Forecast Service uses this as its primary input for analysis.
 */
public record InventorySummaryDto(Long facilityId, String facilityName,
                                   String district, Double latitude, Double longitude,
                                   Long medicineId, String medicineName,
                                   String unitOfMeasurement,
                                   int totalUsableQuantity) {}
