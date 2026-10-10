package com.mediconnect.inventory_service.dto;

public record MedicineDto(Long id, String name, String unitOfMeasurement,
                           boolean essentialMedicine) {}
