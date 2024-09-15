package se.david.microservices.core.shipping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("se.david")
public class ShippingServiceApplication {
  private static final Logger LOG = LoggerFactory.getLogger(ShippingServiceApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(ShippingServiceApplication.class, args);
    String mongoDbHost = context.getEnvironment().getProperty("spring.data.mongodb.host");
    String mongoDbPort = context.getEnvironment().getProperty("spring.data.mongodb.port");
    LOG.info("Connected to MongoDb: {}:{}", mongoDbHost, mongoDbPort);
  }

}
