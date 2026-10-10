package com.mediconnect.inventory_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * A physical batch of medicine held at a facility.
 * One facility can have multiple batches of the same medicine with different
 * expiry dates (e.g., two deliveries received months apart).
 *
 * Key rule: a batch is only "usable" if expiryDate > today.
 * The service layer is responsible for filtering out expired batches.
 */
@Entity
@Table(name = "stock_batch",
       indexes = {
           @Index(name = "idx_batch_facility",  columnList = "facility_id"),
           @Index(name = "idx_batch_medicine",  columnList = "medicine_id"),
           @Index(name = "idx_batch_expiry",    columnList = "expiry_date")
       })
@Getter @Setter @NoArgsConstructor
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    // Supplier's batch/lot number — important for traceability
    @NotBlank
    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    // Current quantity in this batch (updated when stock is consumed or transferred)
    @Min(0)
    @Column(nullable = false)
    private int quantity;

    @NotNull
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;
}
