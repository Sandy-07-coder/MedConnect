package com.mediconnect.inventory_service.dto;

/**
 * What the API returns for a facility. Using a DTO instead of the entity
 * prevents Jackson from trying to serialize Hibernate lazy-loaded collections
 * and avoids infinite recursion (Facility → StockBatch → Facility → ...).
 */
public record FacilityDto(Long id, String name, String district,
                           Double latitude, Double longitude) {}
