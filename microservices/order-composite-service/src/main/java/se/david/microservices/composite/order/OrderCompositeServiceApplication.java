package se.david.microservices.composite.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("se.david")
public class OrderCompositeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderCompositeServiceApplication.class, args);
	}

}
