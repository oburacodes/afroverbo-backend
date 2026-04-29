package com.afroverbo.backend;

import com.afroverbo.backend.config.DarajaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DarajaProperties.class)
public class AfroverboBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfroverboBackendApplication.class, args);
    }
}
