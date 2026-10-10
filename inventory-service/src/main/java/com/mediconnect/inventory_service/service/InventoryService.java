package com.mediconnect.inventory_service.service;

import com.mediconnect.inventory_service.dto.*;
import com.mediconnect.inventory_service.entity.*;
import com.mediconnect.inventory_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * All business logic lives here — controllers are thin wrappers.
 *
 * Key rule: usable stock = SUM of quantities in non-expired batches only.
 * Expired batches are excluded at the repository level (expiryDate > today).
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final FacilityRepository facilityRepo;
    private final MedicineRepository medicineRepo;
    private final StockBatchRepository batchRepo;
    private final ConsumptionRecordRepository consumptionRepo;

    // ─── Facilities ───────────────────────────────────────────────────────────

    public List<FacilityDto> getAllFacilities() {
        return facilityRepo.findAll().stream()
                .map(this::toFacilityDto)
                .collect(Collectors.toList());
    }

    public List<FacilityDto> getFacilitiesByDistrict(String district) {
        return facilityRepo.findByDistrict(district).stream()
                .map(this::toFacilityDto)
                .collect(Collectors.toList());
    }

    // ─── Medicines ────────────────────────────────────────────────────────────

    public List<MedicineDto> getAllMedicines() {
        return medicineRepo.findAll().stream()
                .map(this::toMedicineDto)
                .collect(Collectors.toList());
    }

    // ─── Inventory summary (usable stock aggregated per facility+medicine) ────

    /**
     * Returns one row per facility+medicine pair with their total USABLE stock.
     * Filters: optional district, optional facilityId, optional medicineId.
     * Usable = not expired as of today.
     */
    public List<InventorySummaryDto> getInventory(String district,
                                                   Long facilityId,
                                                   Long medicineId) {
        LocalDate today = LocalDate.now();
        List<Facility> facilities = (district != null && !district.isBlank())
                ? facilityRepo.findByDistrict(district)
                : facilityRepo.findAll();

        return facilities.stream()
                .filter(f -> facilityId == null || f.getId().equals(facilityId))
                .flatMap(f -> {
                    List<Medicine> medicines = (medicineId != null)
                            ? medicineRepo.findById(medicineId).map(List::of).orElse(List.of())
                            : medicineRepo.findAll();

                    return medicines.stream().map(m -> {
                        List<StockBatch> usable = batchRepo.findUsableBatches(f.getId(), m.getId(), today);
                        int total = usable.stream().mapToInt(StockBatch::getQuantity).sum();
                        return new InventorySummaryDto(
                                f.getId(), f.getName(), f.getDistrict(),
                                f.getLatitude(), f.getLongitude(),
                                m.getId(), m.getName(), m.getUnitOfMeasurement(),
                                total);
                    });
                })
                .collect(Collectors.toList());
    }

    // ─── Stock batches ────────────────────────────────────────────────────────

    public List<StockBatchDto> getBatches(Long facilityId, Long medicineId,
                                          Boolean expiredOnly, Integer expiringWithinDays) {
        LocalDate today = LocalDate.now();
        List<StockBatch> batches;

        if (facilityId != null && medicineId != null) {
            batches = batchRepo.findByFacilityIdAndMedicineId(facilityId, medicineId);
        } else if (facilityId != null) {
            batches = batchRepo.findByFacilityId(facilityId);
        } else if (medicineId != null) {
            batches = batchRepo.findByMedicineId(medicineId);
        } else {
            batches = batchRepo.findAll();
        }

        if (Boolean.TRUE.equals(expiredOnly)) {
            batches = batches.stream()
                    .filter(b -> !b.getExpiryDate().isAfter(today))
                    .collect(Collectors.toList());
        }

        if (expiringWithinDays != null) {
            LocalDate cutoff = today.plusDays(expiringWithinDays);
            batches = batches.stream()
                    .filter(b -> b.getExpiryDate().isAfter(today) &&
                                 !b.getExpiryDate().isAfter(cutoff))
                    .collect(Collectors.toList());
        }

        return batches.stream().map(this::toBatchDto).collect(Collectors.toList());
    }

    // ─── Consumption records ──────────────────────────────────────────────────

    public List<ConsumptionRecordDto> getConsumption(Long facilityId, Long medicineId,
                                                      LocalDate from, LocalDate to) {
        List<ConsumptionRecord> records;
        LocalDate start = (from != null) ? from : LocalDate.now().minusDays(30);
        LocalDate end   = (to   != null) ? to   : LocalDate.now();

        if (facilityId != null && medicineId != null) {
            records = consumptionRepo
                    .findByFacilityIdAndMedicineIdAndConsumptionDateBetween(
                            facilityId, medicineId, start, end);
        } else if (facilityId != null) {
            records = consumptionRepo.findByFacilityId(facilityId).stream()
                    .filter(c -> !c.getConsumptionDate().isBefore(start) &&
                                 !c.getConsumptionDate().isAfter(end))
                    .collect(Collectors.toList());
        } else {
            records = consumptionRepo.findAll().stream()
                    .filter(c -> !c.getConsumptionDate().isBefore(start) &&
                                 !c.getConsumptionDate().isAfter(end))
                    .collect(Collectors.toList());
        }
        return records.stream().map(this::toConsumptionDto).collect(Collectors.toList());
    }

    // ─── Mappers (Entity → DTO) ───────────────────────────────────────────────

    private FacilityDto toFacilityDto(Facility f) {
        return new FacilityDto(f.getId(), f.getName(), f.getDistrict(),
                               f.getLatitude(), f.getLongitude());
    }

    private MedicineDto toMedicineDto(Medicine m) {
        return new MedicineDto(m.getId(), m.getName(), m.getUnitOfMeasurement(),
                               m.isEssentialMedicine());
    }

    private StockBatchDto toBatchDto(StockBatch b) {
        return new StockBatchDto(
                b.getId(),
                b.getFacility().getId(), b.getFacility().getName(),
                b.getMedicine().getId(), b.getMedicine().getName(),
                b.getBatchNumber(), b.getQuantity(), b.getExpiryDate(),
                !b.getExpiryDate().isAfter(LocalDate.now()));
    }

    private ConsumptionRecordDto toConsumptionDto(ConsumptionRecord c) {
        return new ConsumptionRecordDto(
                c.getId(),
                c.getFacility().getId(), c.getFacility().getName(),
                c.getMedicine().getId(), c.getMedicine().getName(),
                c.getConsumptionDate(), c.getQuantityConsumed());
    }
}
