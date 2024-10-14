package se.david.microservices.core.shipping.mapper;

import org.mapstruct.Mapper;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.microservices.core.shipping.domain.entity.Shipping;

@Mapper(componentModel = "spring")
public interface ShippingMapper {
  ShippingDto entityToDto(Shipping shipping);

  Shipping createDtoToEntity(ShippingCreateDto shippingCreateDto);

  ShippingCreateDto entityToCreateDto(Shipping shipping);
}
