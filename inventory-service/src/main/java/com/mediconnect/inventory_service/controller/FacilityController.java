package com.mediconnect.inventory_service.controller;

import com.mediconnect.inventory_service.dto.FacilityDto;
import com.mediconnect.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final InventoryService inventoryService;

    /** GET /api/facilities              — all facilities
     *  GET /api/facilities?district=X  — filtered by district */
    @GetMapping
    public List<FacilityDto> getFacilities(
            @RequestParam(required = false) String district) {
        return (district != null && !district.isBlank())
                ? inventoryService.getFacilitiesByDistrict(district)
                : inventoryService.getAllFacilities();
    }
}
