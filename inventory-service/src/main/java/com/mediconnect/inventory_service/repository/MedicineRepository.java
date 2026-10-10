package com.mediconnect.inventory_service.repository;

import com.mediconnect.inventory_service.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    Optional<Medicine> findByNameIgnoreCase(String name);
    List<Medicine> findByEssentialMedicineTrue();
}
