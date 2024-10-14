package se.david.springcloud.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
  private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) throws Exception {
    http
      .csrf(ServerHttpSecurity.CsrfSpec::disable)  // Updated csrf method
      .authorizeExchange(exchange -> exchange  // Replaced authorizeExchange() with authorizeHttpRequests()
        .pathMatchers("/headerrouting/**").permitAll()
        .pathMatchers("/actuator/**").permitAll()
        .pathMatchers("/eureka/**").permitAll()
        .pathMatchers("/oauth2/**").permitAll()
        .pathMatchers("/login/**").permitAll()
        .pathMatchers("/error/**").permitAll()
        .pathMatchers("/openapi/**").permitAll()
        .pathMatchers("/webjars/**").permitAll()
        .anyExchange().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2  // Updated oauth2ResourceServer configuration
        .jwt(Customizer.withDefaults())  // Updated JWT handling
      );

    return http.build();
  }
}
