package com.mediconnect.forecast_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${inventory.service.base-url}")
    private String inventoryBaseUrl;

    /**
     * A single RestClient instance pre-configured with the Inventory Service base URL.
     * Spring 6 RestClient is the modern synchronous HTTP client (replaces RestTemplate).
     */
    @Bean
    public RestClient inventoryRestClient() {
        return RestClient.builder()
                .baseUrl(inventoryBaseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /** Allow the React frontend to call this service */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }
}
