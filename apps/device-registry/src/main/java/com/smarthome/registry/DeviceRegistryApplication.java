package com.smarthome.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeviceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceRegistryApplication.class, args);
    }
}
