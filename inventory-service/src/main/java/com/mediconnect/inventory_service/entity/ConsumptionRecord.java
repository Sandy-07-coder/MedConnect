package com.mediconnect.inventory_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * An immutable daily record of how much of a medicine was consumed at a facility.
 * Think of it as an audit log — you never update or delete these rows.
 * The Forecast Service uses this history to compute average daily demand.
 *
 * Unique constraint: one record per facility+medicine+date combination.
 * If you need to correct a record, you would add a correcting entry in practice.
 */
@Entity
@Table(name = "consumption_record",
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uq_consumption_facility_medicine_date",
               columnNames = {"facility_id", "medicine_id", "consumption_date"}
           )
       },
       indexes = {
           @Index(name = "idx_consumption_facility",  columnList = "facility_id"),
           @Index(name = "idx_consumption_medicine",  columnList = "medicine_id"),
           @Index(name = "idx_consumption_date",      columnList = "consumption_date")
       })
@Getter @Setter @NoArgsConstructor
public class ConsumptionRecord {

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

    @NotNull
    @Column(name = "consumption_date", nullable = false)
    private LocalDate consumptionDate;

    // Quantity dispensed/used that day — cannot be negative
    @Min(0)
    @Column(name = "quantity_consumed", nullable = false)
    private int quantityConsumed;
}
