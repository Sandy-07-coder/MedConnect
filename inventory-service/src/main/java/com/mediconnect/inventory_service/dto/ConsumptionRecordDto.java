package com.mediconnect.inventory_service.dto;

import java.time.LocalDate;

public record ConsumptionRecordDto(Long id, Long facilityId, String facilityName,
                                    Long medicineId, String medicineName,
                                    LocalDate consumptionDate, int quantityConsumed) {}
