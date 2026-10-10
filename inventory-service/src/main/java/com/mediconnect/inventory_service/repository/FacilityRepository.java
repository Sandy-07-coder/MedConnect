package com.mediconnect.inventory_service.repository;

import com.mediconnect.inventory_service.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    // Spring Data auto-generates: SELECT * FROM facility WHERE district = ?
    List<Facility> findByDistrict(String district);
}
