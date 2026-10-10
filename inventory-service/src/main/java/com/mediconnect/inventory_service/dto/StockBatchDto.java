package com.mediconnect.inventory_service.dto;

import java.time.LocalDate;

public record StockBatchDto(Long id, Long facilityId, String facilityName,
                             Long medicineId, String medicineName,
                             String batchNumber, int quantity,
                             LocalDate expiryDate, boolean expired) {}
