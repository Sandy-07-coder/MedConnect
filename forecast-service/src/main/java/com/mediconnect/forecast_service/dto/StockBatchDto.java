package com.mediconnect.forecast_service.dto;

import java.time.LocalDate;

/** Mirrors the StockBatchDto from the Inventory Service. */
public record StockBatchDto(
        Long id,
        Long facilityId,
        String facilityName,
        Long medicineId,
        String medicineName,
        String batchNumber,
        int quantity,
        LocalDate expiryDate,
        boolean expired) {}
