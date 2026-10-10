package com.mediconnect.inventory_service.repository;

import com.mediconnect.inventory_service.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    List<StockBatch> findByFacilityId(Long facilityId);

    List<StockBatch> findByMedicineId(Long medicineId);

    List<StockBatch> findByFacilityIdAndMedicineId(Long facilityId, Long medicineId);

    // Only batches that have NOT yet expired
    @Query("SELECT b FROM StockBatch b WHERE b.facility.id = :facilityId " +
           "AND b.medicine.id = :medicineId AND b.expiryDate > :today")
    List<StockBatch> findUsableBatches(@Param("facilityId") Long facilityId,
                                       @Param("medicineId") Long medicineId,
                                       @Param("today") LocalDate today);

    // Batches expiring within the next N days (expiry risk detection)
    @Query("SELECT b FROM StockBatch b WHERE b.expiryDate BETWEEN :today AND :cutoff " +
           "AND b.quantity > 0 ORDER BY b.expiryDate ASC")
    List<StockBatch> findBatchesExpiringBefore(@Param("today") LocalDate today,
                                               @Param("cutoff") LocalDate cutoff);
}
