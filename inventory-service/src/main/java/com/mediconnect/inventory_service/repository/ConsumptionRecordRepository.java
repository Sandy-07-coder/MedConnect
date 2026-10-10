package com.mediconnect.inventory_service.repository;

import com.mediconnect.inventory_service.entity.ConsumptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ConsumptionRecordRepository extends JpaRepository<ConsumptionRecord, Long> {

    List<ConsumptionRecord> findByFacilityIdAndMedicineIdAndConsumptionDateBetween(
            Long facilityId, Long medicineId, LocalDate from, LocalDate to);

    List<ConsumptionRecord> findByFacilityId(Long facilityId);

    // Average daily consumption over a date range — used by the Forecast Service
    @Query("SELECT AVG(c.quantityConsumed) FROM ConsumptionRecord c " +
           "WHERE c.facility.id = :facilityId AND c.medicine.id = :medicineId " +
           "AND c.consumptionDate BETWEEN :from AND :to")
    Double avgDailyConsumption(@Param("facilityId") Long facilityId,
                               @Param("medicineId") Long medicineId,
                               @Param("from") LocalDate from,
                               @Param("to") LocalDate to);
}
