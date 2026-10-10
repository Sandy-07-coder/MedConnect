package com.mediconnect.forecast_service.model;

/**
 * Stock-out risk classification.
 * Thresholds are PoC prototypes — not official healthcare standards.
 * They are configurable via application.properties.
 */
public enum RiskLevel {
    CRITICAL,   // 0–3 days
    HIGH,       // 4–7 days
    MEDIUM,     // 8–14 days
    LOW         // >14 days
}
