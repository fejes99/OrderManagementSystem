package se.david.microservices.composite.order.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.david.microservices.composite.order.service.integration.OrderCompositeIntegration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class HealthCheckConfiguration {
  @Autowired
  OrderCompositeIntegration integration;

  @Bean
  ReactiveHealthContributor coreServices() {
    final Map<String, ReactiveHealthIndicator> registry = new LinkedHashMap<>();

    registry.put("inventory", () -> integration.getInventoryHealth());
    registry.put("shipping", () -> integration.getShippingHealth());
    registry.put("order", () -> integration.getOrderHealth());

    return CompositeReactiveHealthContributor.fromMap(registry);
  }
}
