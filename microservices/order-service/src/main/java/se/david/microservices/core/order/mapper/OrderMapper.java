package se.david.microservices.core.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderUpdateDto;
import se.david.microservices.core.order.domain.entity.Order;

@Mapper(componentModel = "spring")
public interface OrderMapper {
  OrderDto entityToDto(Order order);
  Order createDtoToEntity(OrderCreateDto orderCreateDto);
  void updateEntityToDto(@MappingTarget Order order, OrderUpdateDto orderUpdateDto);
}
