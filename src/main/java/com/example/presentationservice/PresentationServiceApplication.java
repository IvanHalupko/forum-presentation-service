package com.example.presentationservice;

import com.example.presentationservice.configuration.RestTemplateConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
@Import(RestTemplateConfig.class)
public class PresentationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PresentationServiceApplication.class, args);
    }

}