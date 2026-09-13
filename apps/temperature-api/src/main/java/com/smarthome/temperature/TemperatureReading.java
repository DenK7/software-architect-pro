package com.smarthome.temperature;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TemperatureReading(
        double value,
        String unit,
        String timestamp,
        String location,
        String status,
        @JsonProperty("sensor_id") String sensorId,
        @JsonProperty("sensor_type") String sensorType,
        String description) {
}
