package se.david.microservices.core.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("se.david")
public class InventoryServiceApplication {
  private static final Logger LOG = LoggerFactory.getLogger(InventoryServiceApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(InventoryServiceApplication.class, args);
    String mongoDbHost = context.getEnvironment().getProperty("spring.data.mongodb.host");
    String mongoDbPort = context.getEnvironment().getProperty("spring.data.mongodb.port");
    LOG.info("Connected to MongoDb: {}:{}", mongoDbHost, mongoDbPort);
  }
}
