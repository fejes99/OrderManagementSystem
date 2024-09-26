package se.david.microservices.core.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import se.david.api.core.product.dto.ProductCreateDto;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.product.dto.ProductUpdateDto;
import se.david.microservices.core.product.domain.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {
  ProductDto entityToDto(Product product);
  Product createDtoToEntity(ProductCreateDto productCreateDto);
  void updateEntityWithDto(@MappingTarget Product product, ProductUpdateDto productUpdateDto);
}
