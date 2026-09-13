package com.smarthome.registry;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record MonolithSensor(
        int id,
        String name,
        String type,
        String location,
        double value,
        String unit,
        String status,
        @JsonProperty("last_updated") OffsetDateTime lastUpdated,
        @JsonProperty("created_at") OffsetDateTime createdAt) {
}
