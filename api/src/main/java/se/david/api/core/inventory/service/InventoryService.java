package se.david.api.core.inventory.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.david.api.core.inventory.dto.InventoryCheckRequestDto;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.core.inventory.dto.InventoryReduceRequestDto;

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

  @PutMapping(
    value = "/inventories/checkStock",
    consumes = "application/json",
    produces = "application/json")
  boolean checkStock(@RequestBody List<InventoryCheckRequestDto> inventoryCheckRequests);

  @PutMapping(
    value = "/inventories/reduceStock",
    consumes = "application/json",
    produces = "application/json")
  void reduceStock(@RequestBody List<InventoryReduceRequestDto> inventoryReduceRequests);

}
