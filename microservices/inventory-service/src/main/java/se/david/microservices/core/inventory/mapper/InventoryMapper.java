package se.david.microservices.core.inventory.mapper;

import org.mapstruct.Mapper;
import se.david.api.core.inventory.dto.InventoryCreateDto;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto;
import se.david.microservices.core.inventory.domain.entity.Inventory;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
  InventoryDto entityToDto(Inventory inventory);

  Inventory createDtoToEntity(InventoryCreateDto inventoryCreateDto);

  InventoryStockAdjustmentRequestDto entityToStockAdjustmentRequestDto(Inventory inventory);
}
