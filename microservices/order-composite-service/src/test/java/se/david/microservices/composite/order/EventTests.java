package se.david.microservices.composite.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.event.Event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static se.david.api.event.Event.Type.CREATE;
import static se.david.api.event.Event.Type.UPDATE;
import static se.david.microservices.composite.order.IsSameEvent.sameEventExceptCreatedAt;

public class EventTests {
  ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEventObjectCompare() throws JsonProcessingException {
    Event<Integer, ProductDto> event1 = new Event<>(CREATE, 1, new ProductDto(1, "name", "test", 10, ""));
    Event<Integer, ProductDto> event2 = new Event<>(CREATE, 1, new ProductDto(1, "name", "test", 10, ""));
    Event<Integer, ProductDto> event3 = new Event<>(UPDATE, 1, null);
    Event<Integer, ProductDto> event4 = new Event<>(CREATE, 1, new ProductDto(2, "name", "test", 20, ""));

    String event1Json = mapper.writeValueAsString(event1);

    assertThat(event1Json, is(sameEventExceptCreatedAt(event2)));
    assertThat(event1Json, not(sameEventExceptCreatedAt(event3)));
    assertThat(event1Json, not(sameEventExceptCreatedAt(event4)));
  }
}
