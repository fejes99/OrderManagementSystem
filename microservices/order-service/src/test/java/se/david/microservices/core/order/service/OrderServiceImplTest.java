package se.david.microservices.core.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderItemCreateDto;
import se.david.api.core.order.dto.OrderUpdateDto;
import se.david.api.exceptions.InvalidInputException;
import se.david.api.exceptions.NotFoundException;
import se.david.microservices.core.order.domain.entity.Order;
import se.david.microservices.core.order.domain.entity.OrderItem;
import se.david.microservices.core.order.domain.repository.OrderRepository;
import se.david.microservices.core.order.mapper.OrderItemMapper;
import se.david.microservices.core.order.mapper.OrderMapper;
import se.david.util.http.ServiceUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock
  private OrderRepository repository;

  @Mock
  private ServiceUtil serviceUtil;

  @Mock
  private OrderMapper mapper;

  @Mock
  private OrderItemMapper itemMapper;

  private OrderServiceImpl orderService;

  @BeforeEach
  void setUp() {
    orderService = new OrderServiceImpl(Schedulers.immediate(), repository, serviceUtil, mapper, itemMapper);
  }

  @Test
  @DisplayName("createOrder computes totalPrice as the sum of (item price * quantity) for every line item")
  void createOrderComputesTotalPriceFromItemPriceTimesQuantity() {
    // Regression test for the Phase 0 fix: totalPrice must be the sum of (item price * quantity)
    // for every line item, computed at creation time, rather than being persisted as 0.
    OrderCreateDto createDto = new OrderCreateDto(1,
      List.of(
        new OrderItemCreateDto(101, 2, 500),   // 2 * 500 = 1000
        new OrderItemCreateDto(102, 3, 300)));  // 3 * 300 =  900
                                                 // expected total = 1900

    Order mappedOrder = new Order();
    when(mapper.createDtoToEntity(createDto)).thenReturn(mappedOrder);

    when(itemMapper.createDtoToEntity(any(OrderItemCreateDto.class))).thenAnswer(invocation -> {
      OrderItemCreateDto dto = invocation.getArgument(0);
      return new OrderItem(0, null, dto.productId(), dto.quantity(), dto.price());
    });

    ArgumentCaptor<Order> savedOrderCaptor = ArgumentCaptor.forClass(Order.class);
    when(repository.save(savedOrderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(orderService.createOrder(createDto))
      .expectNextMatches(dto -> dto.totalPrice() == 1900)
      .verifyComplete();

    assertThat(savedOrderCaptor.getValue().getTotalPrice()).isEqualTo(1900);
    assertThat(savedOrderCaptor.getValue().getOrderItems()).hasSize(2);
  }

  @Test
  @DisplayName("createOrder sets the back-reference from each order item to its parent order")
  void createOrderLinksEachOrderItemBackToTheOrder() {
    OrderCreateDto createDto = new OrderCreateDto(1, List.of(new OrderItemCreateDto(101, 1, 100)));
    Order mappedOrder = new Order();
    when(mapper.createDtoToEntity(createDto)).thenReturn(mappedOrder);
    when(itemMapper.createDtoToEntity(any(OrderItemCreateDto.class)))
      .thenReturn(new OrderItem(0, null, 101, 1, 100));
    when(repository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(orderService.createOrder(createDto))
      .expectNextCount(1)
      .verifyComplete();

    assertThat(mappedOrder.getOrderItems()).extracting(OrderItem::getOrder).containsOnly(mappedOrder);
  }

  @Test
  @DisplayName("createOrder throws InvalidInputException for a non-positive userId")
  void createOrderInvalidUserIdThrowsInvalidInputException() {
    OrderCreateDto invalid = new OrderCreateDto(0, List.of(new OrderItemCreateDto(1, 1, 100)));

    assertThrows(InvalidInputException.class, () -> orderService.createOrder(invalid));

    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("getOrder returns the mapped DTO when the order exists")
  void getOrderFoundMapsToDto() {
    Order order = new Order();
    when(repository.findById(1)).thenReturn(Optional.of(order));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(orderService.getOrder(1))
      .expectNextMatches(dto -> dto.serviceAddress().equals("addr"))
      .verifyComplete();
  }

  @Test
  @DisplayName("getOrder emits NotFoundException when the order does not exist")
  void getOrderNotFoundEmitsNotFoundException() {
    when(repository.findById(1)).thenReturn(Optional.empty());

    StepVerifier.create(orderService.getOrder(1))
      .expectError(NotFoundException.class)
      .verify();
  }

  @Test
  @DisplayName("getOrdersByUser throws InvalidInputException for a non-positive userId")
  void getOrdersByUserInvalidUserIdThrowsInvalidInputException() {
    assertThrows(InvalidInputException.class, () -> orderService.getOrdersByUser(0));

    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("updateOrder emits NotFoundException and never saves when the order does not exist")
  void updateOrderNotFoundEmitsNotFoundException() {
    when(repository.findById(99)).thenReturn(Optional.empty());

    StepVerifier.create(orderService.updateOrder(99, new OrderUpdateDto("SHIPPED")))
      .expectError(NotFoundException.class)
      .verify();

    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("deleteOrder deletes the entity when it exists")
  void deleteOrderFoundDeletesEntity() {
    Order order = new Order();
    when(repository.findById(1)).thenReturn(Optional.of(order));

    StepVerifier.create(orderService.deleteOrder(1))
      .verifyComplete();

    verify(repository).delete(order);
  }
}
