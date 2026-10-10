package com.mediconnect.forecast_service.dto;

import java.time.LocalDate;

/** Mirrors the ConsumptionRecordDto from the Inventory Service. */
public record ConsumptionRecordDto(
        Long id,
        Long facilityId,
        String facilityName,
        Long medicineId,
        String medicineName,
        LocalDate consumptionDate,
        int quantityConsumed) {}
