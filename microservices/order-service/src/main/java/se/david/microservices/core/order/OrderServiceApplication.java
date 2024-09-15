package se.david.microservices.core.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("se.david")
public class OrderServiceApplication {
  private static final Logger LOG = LoggerFactory.getLogger(OrderServiceApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(OrderServiceApplication.class, args);

    String mysqlUri = context.getEnvironment().getProperty("spring.datasource.url");
    LOG.info("Connected to MySQL: {}", mysqlUri);
  }

}
