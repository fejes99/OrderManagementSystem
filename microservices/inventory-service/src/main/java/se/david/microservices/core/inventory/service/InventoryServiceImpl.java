package se.david.microservices.core.inventory.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.core.inventory.service.InventoryService;
import se.david.util.http.ServiceUtil;

import java.util.List;

@RestController
public class InventoryServiceImpl implements InventoryService {
  private final ServiceUtil serviceUtil;

  @Autowired
  public InventoryServiceImpl(ServiceUtil serviceUtil) {
    this.serviceUtil = serviceUtil;
  }

  @Override
  public List<InventoryDto> getInventoryStocks() {
    return List.of();
  }

  @Override
  public InventoryDto getInventoryStock(int productId) {
    return new InventoryDto(1, 10, serviceUtil.getServiceAddress());
  }

  @Override
  public InventoryDto updateInventoryStock(int productId, InventoryDto inventory) {
    return null;
  }
}
