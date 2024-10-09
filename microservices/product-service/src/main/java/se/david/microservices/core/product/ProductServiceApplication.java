package se.david.microservices.core.product;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ComponentScan("se.david")
public class ProductServiceApplication {
  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceApplication.class);
  private final Integer threadPoolSize;
  private final Integer taskQueueSize;

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

  @Autowired
  public ProductServiceApplication(
    @Value("${app.threadPoolSize:10}") Integer threadPoolSize,
    @Value("${app.taskQueueSize:100}") Integer taskQueueSize) {
    this.threadPoolSize = threadPoolSize;
    this.taskQueueSize = taskQueueSize;
  }

  @Bean
  public Scheduler jdbcScheduler() {
    LOG.info("Creates a jdbcScheduler with thread pool size = {}", threadPoolSize);
    return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "jdbc-pool");
  }

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(ProductServiceApplication.class, args);

    String mysqlUri = context.getEnvironment().getProperty("spring.datasource.url");
    LOG.info("Connected to MySQL: {}", mysqlUri);
  }

}
