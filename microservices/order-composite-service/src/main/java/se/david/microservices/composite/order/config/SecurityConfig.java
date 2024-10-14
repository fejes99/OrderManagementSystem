package se.david.microservices.composite.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

    http
      .authorizeExchange(exchange -> exchange  // Replaced authorizeExchange() with authorizeHttpRequests()
        .pathMatchers("/openapi/**").permitAll()
        .pathMatchers("/webjars/**").permitAll()
        .pathMatchers("/actuator/**").permitAll()
        .pathMatchers(POST, "/order-composite/**").hasAuthority("SCOPE_order:write")
        .pathMatchers(GET, "/order-composite/**").hasAuthority("SCOPE_order:read")
        .anyExchange().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2  // Updated oauth2ResourceServer configuration
        .jwt(Customizer.withDefaults())  // Updated JWT handling
      );

    return http.build();
  }
}
