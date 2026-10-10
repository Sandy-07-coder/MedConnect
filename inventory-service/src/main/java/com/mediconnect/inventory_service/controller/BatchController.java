package com.mediconnect.inventory_service.controller;

import com.mediconnect.inventory_service.dto.StockBatchDto;
import com.mediconnect.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final InventoryService inventoryService;

    /**
     * GET /api/batches
     * GET /api/batches?facilityId=1
     * GET /api/batches?medicineId=2
     * GET /api/batches?expiredOnly=true
     * GET /api/batches?expiringWithinDays=30
     */
    @GetMapping
    public List<StockBatchDto> getBatches(
            @RequestParam(required = false) Long facilityId,
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) Boolean expiredOnly,
            @RequestParam(required = false) Integer expiringWithinDays) {
        return inventoryService.getBatches(facilityId, medicineId, expiredOnly, expiringWithinDays);
    }
}
