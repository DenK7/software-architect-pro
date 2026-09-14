package com.smarthome.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class MonolithPoller {

    private static final Logger log = LoggerFactory.getLogger(MonolithPoller.class);

    private final RestClient client;
    private final DeviceRepository repository;

    public MonolithPoller(DeviceRepository repository, @Value("${monolith.url}") String monolithUrl) {
        this.repository = repository;
        this.client = RestClient.builder().baseUrl(monolithUrl).build();
    }

    @Scheduled(fixedDelayString = "${monolith.poll-interval-ms}")
    public void synchronize() {
        List<MonolithSensor> sensors;
        try {
            sensors = client.get()
                    .uri("/api/v1/sensors")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MonolithSensor>>() {});
        } catch (Exception e) {
            log.warn("Монолит недоступен, реестр оставлен без изменений: {}", e.getMessage());
            return;
        }

        if (sensors == null) {
            sensors = List.of();
        }

        sensors.forEach(repository::upsert);
        int removed = repository.markMissingDeleted(sensors.stream().map(MonolithSensor::id).toList());

        log.info("Синхронизация: получено {}, помечено удалёнными {}", sensors.size(), removed);
    }
}
