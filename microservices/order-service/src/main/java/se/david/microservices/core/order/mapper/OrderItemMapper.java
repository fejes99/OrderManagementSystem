package se.david.microservices.core.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import se.david.api.composite.order.dto.OrderItemCreateDto;
import se.david.api.core.order.dto.OrderItemDto;
import se.david.microservices.core.order.domain.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
  @Mapping(target = "orderId", source = "order.id")
  OrderItemDto entityToDto(OrderItem orderItem);

  OrderItemCreateDto entityToCreateDto(OrderItem orderItem);

  OrderItem createDtoToEntity(OrderItemCreateDto orderItemCreateDto);
}
