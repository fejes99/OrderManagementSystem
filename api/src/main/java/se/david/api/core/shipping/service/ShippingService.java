package se.david.api.core.shipping.service;

import org.springframework.web.bind.annotation.*;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.core.shipping.dto.ShippingDto;

import java.util.List;

public interface ShippingService {
  @GetMapping(
    value = "/shipments",
    produces = "application/json")
  List<ShippingDto> getShipments();

  @GetMapping(
    value = "/shipments/byIds",
    produces = "application/json")
  List<ShippingDto> getShipmentsByIds(@RequestParam List<Integer> ids);

  @GetMapping(
    value = "/shipments/{orderId}",
    produces = "application/json")
  ShippingDto getShipping(@PathVariable int orderId);

  @PostMapping(
    value = "/shipments",
    consumes = "application/json",
    produces = "application/json")
  ShippingDto createShippingOrder(@RequestBody ShippingCreateDto shipping);

  @PutMapping(
    value = "/shipments/{shippingId}",
    consumes = "application/json",
    produces = "application/json")
  ShippingDto updateShippingStatus(@PathVariable int shippingId, @RequestBody String status);

  @DeleteMapping(
    value = "/shipments/{shippingId}")
  void deleteShipping(@PathVariable int shippingId);
}
