package com.smarthome.registry;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record Device(
        int id,
        String name,
        String type,
        String location,
        String unit,
        String status,
        @JsonProperty("registered_at") OffsetDateTime registeredAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        @JsonProperty("deleted_at") OffsetDateTime deletedAt,
        @JsonProperty("deleted_by") String deletedBy) {
}
