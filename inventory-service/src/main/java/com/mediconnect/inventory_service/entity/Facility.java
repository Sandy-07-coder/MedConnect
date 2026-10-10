package com.mediconnect.inventory_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a healthcare facility (PHC, district hospital, etc.).
 * One facility can hold many stock batches and consumption records.
 */
@Entity
@Table(name = "facility")
@Getter @Setter @NoArgsConstructor
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String district;

    // Latitude/longitude for distance calculations in the Forecast Service
    private Double latitude;
    private Double longitude;

    // Bidirectional: one facility → many batches and consumption records
    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL)
    private List<StockBatch> stockBatches;

    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL)
    private List<ConsumptionRecord> consumptionRecords;
}
