package se.david.microservices.core.inventory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("se.david")
public class InventoryServiceApplication {
  private static final Logger LOG = LoggerFactory.getLogger(InventoryServiceApplication.class);

  @Value("${api.common.version}")
  private String apiVersion;

  @Value("${api.common.title}")
  private String apiTitle;

  @Value("${api.common.description}")
  private String apiDescription;

  @Bean
  public OpenAPI getOpenApiDocumentation() {
    return new OpenAPI()
      .info(new Info()
        .title(apiTitle)
        .description(apiDescription)
        .version(apiVersion));
  }

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(InventoryServiceApplication.class, args);
    String mongoDbHost = context.getEnvironment().getProperty("spring.data.mongodb.host");
    String mongoDbPort = context.getEnvironment().getProperty("spring.data.mongodb.port");
    LOG.info("Connected to MongoDb: {}:{}", mongoDbHost, mongoDbPort);
  }
}
