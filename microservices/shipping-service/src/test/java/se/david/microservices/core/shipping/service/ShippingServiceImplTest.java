package se.david.microservices.core.shipping.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.exceptions.InvalidInputException;
import se.david.api.exceptions.NotFoundException;
import se.david.microservices.core.shipping.domain.entity.Shipping;
import se.david.microservices.core.shipping.domain.repository.ShippingRepository;
import se.david.microservices.core.shipping.mapper.ShippingMapper;
import se.david.util.http.ServiceUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

  @Mock
  private ShippingRepository repository;

  @Mock
  private ServiceUtil serviceUtil;

  @Mock
  private ShippingMapper mapper;

  private ShippingServiceImpl shippingService;

  @BeforeEach
  void setUp() {
    shippingService = new ShippingServiceImpl(repository, serviceUtil, mapper);
  }

  @Test
  @DisplayName("getShippingByOrderId returns the mapped DTO when the shipment exists")
  void getShippingByOrderIdFoundMapsToDto() {
    Shipping shipping = new Shipping(1, "123 Main St", "Dispatched");
    when(repository.findByOrderId(1)).thenReturn(Mono.just(shipping));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(shippingService.getShippingByOrderId(1))
      .expectNextMatches(dto -> dto.orderId() == 1 && dto.status().equals("Dispatched"))
      .verifyComplete();
  }

  @Test
  @DisplayName("getShippingByOrderId emits NotFoundException when no shipment exists")
  void getShippingByOrderIdNotFoundEmitsNotFoundException() {
    when(repository.findByOrderId(1)).thenReturn(Mono.empty());

    StepVerifier.create(shippingService.getShippingByOrderId(1))
      .expectError(NotFoundException.class)
      .verify();
  }

  @Test
  @DisplayName("getShippingByOrderId throws InvalidInputException for a non-positive orderId")
  void getShippingByOrderIdInvalidIdThrowsInvalidInputException() {
    assertThrows(InvalidInputException.class, () -> shippingService.getShippingByOrderId(0));

    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("createShippingOrder saves the mapped entity and returns its DTO")
  void createShippingOrderSavesMappedEntity() {
    ShippingCreateDto createDto = new ShippingCreateDto(1, "123 Main St");
    Shipping entity = new Shipping(1, "123 Main St", "Dispatched");
    when(mapper.createDtoToEntity(createDto)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(Mono.just(entity));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(shippingService.createShippingOrder(createDto))
      .expectNextMatches(dto -> dto.orderId() == 1 && dto.shippingAddress().equals("123 Main St"))
      .verifyComplete();
  }

  @Test
  @DisplayName("updateShippingStatusByOrderId updates the status and saves the entity")
  void updateShippingStatusByOrderIdUpdatesStatus() {
    Shipping shipping = new Shipping(1, "123 Main St", "Dispatched");
    when(repository.findByOrderId(1)).thenReturn(Mono.just(shipping));
    when(repository.save(shipping)).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(shippingService.updateShippingStatusByOrderId(1, "DELIVERED"))
      .expectNextMatches(dto -> dto.status().equals("DELIVERED"))
      .verifyComplete();
  }

  @Test
  @DisplayName("updateShippingStatusByOrderId emits NotFoundException when no shipment exists")
  void updateShippingStatusByOrderIdNotFoundEmitsNotFoundException() {
    when(repository.findByOrderId(1)).thenReturn(Mono.empty());

    StepVerifier.create(shippingService.updateShippingStatusByOrderId(1, "DELIVERED"))
      .expectError(NotFoundException.class)
      .verify();

    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("getShipmentsByOrderIds delegates to repository.findByOrderIdIn")
  void getShipmentsByOrderIdsDelegatesToFindByOrderIdIn() {
    Shipping shipping = new Shipping(5, "addr", "Dispatched");
    when(repository.findByOrderIdIn(List.of(5))).thenReturn(reactor.core.publisher.Flux.just(shipping));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(shippingService.getShipmentsByOrderIds(List.of(5)))
      .expectNextMatches(dto -> dto.orderId() == 5)
      .verifyComplete();

    verify(repository).findByOrderIdIn(List.of(5));
  }
}
