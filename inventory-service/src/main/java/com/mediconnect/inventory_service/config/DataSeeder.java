package com.mediconnect.inventory_service.config;

import com.mediconnect.inventory_service.entity.*;
import com.mediconnect.inventory_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Loads deterministic sample data ONLY when the database is empty.
 * Safe to run on every restart — it checks before inserting.
 *
 * District: Vellore
 * Facilities: PHC-A (shortage), PHC-B (moderate), PHC-C (surplus), PHC-D (surplus)
 * Medicines: Paracetamol, Amoxicillin, ORS
 *
 * This data is designed so the Forecast Service can immediately demonstrate:
 * - A critical stock-out risk at PHC-A
 * - A viable donor at PHC-D
 * - An expiry-risk batch at PHC-C
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final FacilityRepository facilityRepo;
    private final MedicineRepository medicineRepo;
    private final StockBatchRepository batchRepo;
    private final ConsumptionRecordRepository consumptionRepo;

    @Override
    public void run(ApplicationArguments args) {
        if (facilityRepo.count() > 0) {
            log.info("Seed data already present — skipping.");
            return;
        }
        log.info("Seeding sample data...");

        // ── Facilities ────────────────────────────────────────────────────────
        Facility phcA = facility("PHC-A", "Vellore", 12.9165, 79.1325);
        Facility phcB = facility("PHC-B", "Vellore", 12.9341, 79.1586);
        Facility phcC = facility("PHC-C", "Vellore", 12.8956, 79.1442);
        Facility phcD = facility("PHC-D", "Vellore", 12.9523, 79.1267);
        Facility distH = facility("District Hospital", "Vellore", 12.9242, 79.1347);

        // ── Medicines ─────────────────────────────────────────────────────────
        Medicine paracetamol = medicine("Paracetamol 500mg", "tablet", true);
        Medicine amoxicillin = medicine("Amoxicillin 250mg", "capsule", true);
        Medicine ors          = medicine("ORS Sachet", "sachet", true);

        LocalDate today = LocalDate.now();

        // ── Stock batches (PHC-A is critically low) ───────────────────────────
        // PHC-A: 240 paracetamol — only ~5.7 days at 42/day → CRITICAL
        batch(phcA, paracetamol, "BATCH-PA-001", 240, today.plusDays(120));
        batch(phcA, amoxicillin, "BATCH-AA-001", 150, today.plusDays(90));
        batch(phcA, ors,         "BATCH-OA-001", 80,  today.plusDays(60));

        // PHC-B: moderate stock
        batch(phcB, paracetamol, "BATCH-PB-001", 600, today.plusDays(200));
        batch(phcB, amoxicillin, "BATCH-AB-001", 300, today.plusDays(150));
        batch(phcB, ors,         "BATCH-OB-001", 200, today.plusDays(180));

        // PHC-C: has an expiry-risk batch of paracetamol (expires in 20 days, qty 500)
        batch(phcC, paracetamol, "BATCH-PC-001", 500, today.plusDays(20));  // ← expiry risk
        batch(phcC, paracetamol, "BATCH-PC-002", 400, today.plusDays(300));
        batch(phcC, amoxicillin, "BATCH-AC-001", 250, today.plusDays(120));
        batch(phcC, ors,         "BATCH-OC-001", 350, today.plusDays(90));

        // PHC-D: surplus paracetamol — 1200 tablets at 30/day = 40 days → DONOR
        batch(phcD, paracetamol, "BATCH-PD-001", 1200, today.plusDays(365));
        batch(phcD, amoxicillin, "BATCH-AD-001", 500,  today.plusDays(200));
        batch(phcD, ors,         "BATCH-OD-001", 600,  today.plusDays(270));

        // District Hospital: large stock
        batch(distH, paracetamol, "BATCH-PH-001", 2000, today.plusDays(400));
        batch(distH, amoxicillin, "BATCH-AH-001", 1000, today.plusDays(300));
        batch(distH, ors,         "BATCH-OH-001", 1500, today.plusDays(350));

        // ── Consumption records (last 30 days) ────────────────────────────────
        // PHC-A: ~42 paracetamol/day (high demand, causes critical risk)
        seedConsumption(phcA, paracetamol, today, 30, 38, 45);
        seedConsumption(phcA, amoxicillin, today, 30, 10, 15);
        seedConsumption(phcA, ors,         today, 30, 5,  8);

        // PHC-B: moderate consumption
        seedConsumption(phcB, paracetamol, today, 30, 25, 35);
        seedConsumption(phcB, amoxicillin, today, 30, 12, 18);
        seedConsumption(phcB, ors,         today, 30, 8,  12);

        // PHC-C: lower consumption (causes expiry risk on old batch)
        seedConsumption(phcC, paracetamol, today, 30, 18, 25);
        seedConsumption(phcC, amoxicillin, today, 30, 8,  12);
        seedConsumption(phcC, ors,         today, 30, 10, 15);

        // PHC-D: 30/day paracetamol — has surplus for transfer
        seedConsumption(phcD, paracetamol, today, 30, 27, 33);
        seedConsumption(phcD, amoxicillin, today, 30, 15, 20);
        seedConsumption(phcD, ors,         today, 30, 12, 18);

        // District Hospital
        seedConsumption(distH, paracetamol, today, 30, 60, 80);
        seedConsumption(distH, amoxicillin, today, 30, 30, 40);
        seedConsumption(distH, ors,         today, 30, 20, 30);

        log.info("Seed data loaded successfully.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Facility facility(String name, String district, double lat, double lon) {
        Facility f = new Facility();
        f.setName(name);
        f.setDistrict(district);
        f.setLatitude(lat);
        f.setLongitude(lon);
        return facilityRepo.save(f);
    }

    private Medicine medicine(String name, String unit, boolean essential) {
        Medicine m = new Medicine();
        m.setName(name);
        m.setUnitOfMeasurement(unit);
        m.setEssentialMedicine(essential);
        return medicineRepo.save(m);
    }

    private void batch(Facility f, Medicine m, String batchNo, int qty, LocalDate expiry) {
        StockBatch b = new StockBatch();
        b.setFacility(f);
        b.setMedicine(m);
        b.setBatchNumber(batchNo);
        b.setQuantity(qty);
        b.setExpiryDate(expiry);
        batchRepo.save(b);
    }

    /**
     * Seeds daily consumption for the past `days` days.
     * Quantity varies randomly between minQty and maxQty to simulate realistic variation.
     */
    private void seedConsumption(Facility f, Medicine m, LocalDate today,
                                  int days, int minQty, int maxQty) {
        for (int i = days; i >= 1; i--) {
            LocalDate date = today.minusDays(i);
            int qty = minQty + (int)(Math.random() * (maxQty - minQty + 1));
            ConsumptionRecord cr = new ConsumptionRecord();
            cr.setFacility(f);
            cr.setMedicine(m);
            cr.setConsumptionDate(date);
            cr.setQuantityConsumed(qty);
            consumptionRepo.save(cr);
        }
    }
}
