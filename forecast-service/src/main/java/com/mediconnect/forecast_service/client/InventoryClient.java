package com.mediconnect.forecast_service.client;

import com.mediconnect.forecast_service.dto.ConsumptionRecordDto;
import com.mediconnect.forecast_service.dto.InventorySummaryDto;
import com.mediconnect.forecast_service.dto.StockBatchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

/**
 * All HTTP calls to the Inventory Service live here.
 * The Forecast Service never reads the database directly — only through this client.
 *
 * Error handling strategy:
 *   - If the Inventory Service is unreachable (ResourceAccessException), we catch it,
 *     log it, and return an empty list. The calling service layer will detect the empty
 *     result and return a structured 503 error to the API consumer.
 *   - We do NOT silently return stale cached data — that would hide the real problem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestClient inventoryRestClient;

    /** Fetch aggregated usable inventory, optionally filtered */
    public List<InventorySummaryDto> getInventory(String district, Long facilityId, Long medicineId) {
        try {
            String uri = UriComponentsBuilder.fromPath("/api/inventory")
                    .queryParamIfPresent("district",   java.util.Optional.ofNullable(district))
                    .queryParamIfPresent("facilityId", java.util.Optional.ofNullable(facilityId))
                    .queryParamIfPresent("medicineId", java.util.Optional.ofNullable(medicineId))
                    .toUriString();

            return inventoryRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<InventorySummaryDto>>() {});
        } catch (ResourceAccessException ex) {
            log.error("Inventory Service unreachable when fetching inventory: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /** Fetch all stock batches for a facility+medicine, including expiry metadata */
    public List<StockBatchDto> getBatches(Long facilityId, Long medicineId) {
        try {
            String uri = UriComponentsBuilder.fromPath("/api/batches")
                    .queryParamIfPresent("facilityId", java.util.Optional.ofNullable(facilityId))
                    .queryParamIfPresent("medicineId", java.util.Optional.ofNullable(medicineId))
                    .toUriString();

            return inventoryRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<StockBatchDto>>() {});
        } catch (ResourceAccessException ex) {
            log.error("Inventory Service unreachable when fetching batches: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /** Fetch consumption records for a facility+medicine over a date range */
    public List<ConsumptionRecordDto> getConsumption(Long facilityId, Long medicineId,
                                                      String from, String to) {
        try {
            String uri = UriComponentsBuilder.fromPath("/api/consumption")
                    .queryParamIfPresent("facilityId", java.util.Optional.ofNullable(facilityId))
                    .queryParamIfPresent("medicineId", java.util.Optional.ofNullable(medicineId))
                    .queryParamIfPresent("from", java.util.Optional.ofNullable(from))
                    .queryParamIfPresent("to", java.util.Optional.ofNullable(to))
                    .toUriString();

            return inventoryRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ConsumptionRecordDto>>() {});
        } catch (ResourceAccessException ex) {
            log.error("Inventory Service unreachable when fetching consumption: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /** Fetch batches expiring within the next N days across all facilities */
    public List<StockBatchDto> getBatchesExpiringWithin(int days) {
        try {
            return inventoryRestClient.get()
                    .uri("/api/batches?expiringWithinDays=" + days)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<StockBatchDto>>() {});
        } catch (ResourceAccessException ex) {
            log.error("Inventory Service unreachable when fetching expiry batches: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
