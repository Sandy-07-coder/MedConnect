package com.mediconnect.forecast_service.controller;

import com.mediconnect.forecast_service.model.FacilityAnalysis;
import com.mediconnect.forecast_service.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * GET /api/analysis
     * GET /api/analysis?district=Vellore&medicineId=1&forecastHorizonDays=14
     *
     * Returns one FacilityAnalysis per facility+medicine pair, sorted by risk (CRITICAL first).
     * The Inventory Service must be running — returns 503 if unreachable.
     */
    @GetMapping("/analysis")
    public List<FacilityAnalysis> getAnalysis(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Long medicineId,
            @RequestParam(defaultValue = "14") int forecastHorizonDays) {
        return analysisService.analyse(district, medicineId, forecastHorizonDays);
    }

    /**
     * GET /api/dashboard/summary
     *
     * Returns aggregate statistics for the top-of-page dashboard cards:
     *  - facilitiesMonitored
     *  - facilitiesAtRisk
     *  - batchesWithExpiryRisk
     *  - pendingRedistributionRecommendations
     *  - generatedAt
     */
    @GetMapping("/dashboard/summary")
    public Map<String, Object> getDashboardSummary() {
        return analysisService.getDashboardSummary();
    }
}
