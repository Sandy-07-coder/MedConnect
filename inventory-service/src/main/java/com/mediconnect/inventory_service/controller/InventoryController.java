package com.mediconnect.inventory_service.controller;

import com.mediconnect.inventory_service.dto.InventorySummaryDto;
import com.mediconnect.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * GET /api/inventory
     * GET /api/inventory?district=Vellore
     * GET /api/inventory?facilityId=1&medicineId=2
     *
     * Returns aggregated usable (non-expired) stock per facility+medicine.
     * This is the primary endpoint consumed by the Forecast Service.
     */
    @GetMapping
    public List<InventorySummaryDto> getInventory(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Long facilityId,
            @RequestParam(required = false) Long medicineId) {
        return inventoryService.getInventory(district, facilityId, medicineId);
    }
}
