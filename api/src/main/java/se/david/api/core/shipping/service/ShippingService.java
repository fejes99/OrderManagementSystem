package se.david.api.core.shipping.service;

import org.springframework.web.bind.annotation.*;
import se.david.api.core.shipping.dto.ShippingDto;

import java.util.List;

public interface ShippingService {
  @GetMapping(
    value = "/shipments",
    produces = "application/json")
  List<ShippingDto> getShipments();

  @GetMapping(
    value = "/shipments/{shippingId}",
    produces = "application/json")
  ShippingDto getShipping(@PathVariable int orderId);

  @PostMapping(
    value = "/shipments",
    consumes = "application/json",
    produces = "application/json")
  ShippingDto createShippingOrder(@RequestBody ShippingDto shipping);

  @PutMapping(
    value = "/shipments/{shippingId}",
    consumes = "application/json",
    produces = "application/json")
  ShippingDto updateShippingStatus(@PathVariable int orderId, @RequestBody String status);

  @DeleteMapping(
    value = "/shipments/{shippingId}")
  void deleteShipping(@PathVariable int shippingId);
}
