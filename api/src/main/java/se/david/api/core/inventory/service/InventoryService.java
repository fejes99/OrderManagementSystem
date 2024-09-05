package se.david.api.core.inventory.service;

import org.springframework.web.bind.annotation.*;
import se.david.api.core.inventory.dto.InventoryDto;

import java.util.List;

public interface InventoryService {
  @GetMapping(
    value = "/inventories",
    produces = "application/json")
  List<InventoryDto> getInventoryStocks();

  @GetMapping(
    value = "/inventories/{productId}",
    produces = "application/json")
  InventoryDto getInventoryStock(@PathVariable int productId);

  @PutMapping(
    value = "/inventories/{productId}",
    consumes = "application/json",
    produces = "application/json")
  InventoryDto updateInventoryStock(@PathVariable int productId, @RequestBody InventoryDto inventory);
}
