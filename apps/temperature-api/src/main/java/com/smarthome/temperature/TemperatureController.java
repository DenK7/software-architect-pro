package com.smarthome.temperature;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class TemperatureController {

    private static final double MIN_CELSIUS = 18.0;
    private static final double MAX_CELSIUS = 26.0;

    private static final Map<String, String> LOCATION_BY_SENSOR = Map.of(
            "1", "Living Room",
            "2", "Bedroom",
            "3", "Kitchen");

    private static final Map<String, String> SENSOR_BY_LOCATION = Map.of(
            "Living Room", "1",
            "Bedroom", "2",
            "Kitchen", "3");

    @GetMapping("/temperature")
    public TemperatureReading byLocation(
            @RequestParam(required = false) String location,
            @RequestParam(name = "sensor_id", required = false) String sensorId) {
        return reading(location, sensorId);
    }

   @GetMapping("/temperature/{sensorId}")
    public TemperatureReading bySensorId(@PathVariable String sensorId) {
        return reading(null, sensorId);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    private TemperatureReading reading(String location, String sensorId) {
        String room = blankToEmpty(location);
        String id = blankToEmpty(sensorId);

        if (room.isEmpty()) {
            room = LOCATION_BY_SENSOR.getOrDefault(id, "Unknown");
        }
        if (id.isEmpty()) {
            id = SENSOR_BY_LOCATION.getOrDefault(room, "0");
        }

        return new TemperatureReading(
                randomCelsius(),
                "°C",
                Instant.now().toString(),
                room,
                "active",
                id,
                "temperature",
                "Случайное показание датчика температуры, комната " + room);
    }

    private static double randomCelsius() {
        double value = ThreadLocalRandom.current().nextDouble(MIN_CELSIUS, MAX_CELSIUS);
        return Math.round(value * 100.0) / 100.0;
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
