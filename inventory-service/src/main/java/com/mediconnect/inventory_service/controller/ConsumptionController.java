package com.mediconnect.inventory_service.controller;

import com.mediconnect.inventory_service.dto.ConsumptionRecordDto;
import com.mediconnect.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/consumption")
@RequiredArgsConstructor
public class ConsumptionController {

    private final InventoryService inventoryService;

    /**
     * GET /api/consumption
     * GET /api/consumption?facilityId=1&medicineId=2&from=2026-09-01&to=2026-09-30
     *
     * Defaults: last 30 days if no date range supplied.
     */
    @GetMapping
    public List<ConsumptionRecordDto> getConsumption(
            @RequestParam(required = false) Long facilityId,
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return inventoryService.getConsumption(facilityId, medicineId, from, to);
    }
}
