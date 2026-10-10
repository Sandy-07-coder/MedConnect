package com.mediconnect.inventory_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Master record for a medicine (e.g. Paracetamol 500mg tablet).
 * One medicine can appear in many stock batches across many facilities.
 */
@Entity
@Table(name = "medicine")
@Getter @Setter @NoArgsConstructor
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    // e.g. "tablet", "vial", "ml"
    @NotBlank
    @Column(name = "unit_of_measurement", nullable = false)
    private String unitOfMeasurement;

    // True if this is on the Essential Medicines List
    @Column(name = "essential_medicine", nullable = false)
    private boolean essentialMedicine;

    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL)
    private List<StockBatch> stockBatches;

    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL)
    private List<ConsumptionRecord> consumptionRecords;
}
