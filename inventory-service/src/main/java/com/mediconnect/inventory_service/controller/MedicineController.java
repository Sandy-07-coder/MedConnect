package com.mediconnect.inventory_service.controller;

import com.mediconnect.inventory_service.dto.MedicineDto;
import com.mediconnect.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final InventoryService inventoryService;

    /** GET /api/medicines — full medicine master list */
    @GetMapping
    public List<MedicineDto> getMedicines() {
        return inventoryService.getAllMedicines();
    }
}
