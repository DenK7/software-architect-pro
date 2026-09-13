package com.smarthome.registry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DeviceController {

    private final DeviceRepository repository;

    public DeviceController(DeviceRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/v1/devices")
    public List<Device> list(@RequestParam(name = "include_deleted", defaultValue = "false") boolean includeDeleted) {
        return repository.findAll(includeDeleted);
    }

    @GetMapping("/api/v1/devices/{id}")
    public ResponseEntity<Device> byId(@PathVariable int id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
