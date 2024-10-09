package se.david.api.core.shipping.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.core.shipping.dto.ShippingDto;

import java.util.List;

@Tag(name = "Shipping Service", description = "REST API for managing shipping orders and status.")
public interface ShippingService {

  @GetMapping(
    value = "/shipments",
    produces = "application/json")
  @Operation(
    summary = "Get all shipments",
    description = "Retrieves a list of all shipment records.",
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the list of shipments",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShippingDto.class, type = "array")
        )
      )
    }
  )
  Flux<ShippingDto> getShipments();

  @GetMapping(
    value = "/shipments/byOrdersIds",
    produces = "application/json")
  @Operation(
    summary = "Get shipments by Order IDs",
    description = "Retrieves shipment details for a list of order IDs.",
    parameters = {
      @Parameter(
        name = "orderIds",
        in = ParameterIn.QUERY,
        required = true,
        description = "List of order IDs to retrieve shipments for",
        array = @ArraySchema(schema = @Schema(implementation = Integer.class))
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the shipments for the specified order IDs",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShippingDto.class, type = "array")
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "One or more shipments not found for the specified order IDs"
      )
    }
  )
  Flux<ShippingDto> getShipmentsByOrderIds(@RequestParam List<Integer> orderIds);

  @GetMapping(
    value = "/shipments/order/{orderId}",
    produces = "application/json")
  @Operation(
    summary = "Get shipment by Order ID",
    description = "Retrieves the shipment details for a specific order identified by the order ID.",
    parameters = {
      @Parameter(
        name = "orderId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the order to retrieve the shipment for",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the shipment information",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShippingDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Shipment with the specified order ID not found"
      )
    }
  )
  Mono<ShippingDto> getShippingByOrderId(@PathVariable int orderId);

  @PostMapping(
    value = "/shipments",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Create a new shipping order",
    description = "Creates a new shipping order with the specified details. The request body should contain the shipment's details.",
    responses = {
      @ApiResponse(
        responseCode = "201",
        description = "Successfully created the shipment",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShippingCreateDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request body or shipment details"
      )
    }
  )
  Mono<ShippingDto> createShippingOrder(@RequestBody ShippingCreateDto shippingCreateDto);

  @PutMapping(
    value = "/shipments/order/{orderId}",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Update shipping status by Order ID",
    description = "Updates the status of a specific shipment identified by the Order ID.",
    parameters = {
      @Parameter(
        name = "orderId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the order to update",
        schema = @Schema(type = "integer")
      ),
      @Parameter(
        name = "status",
        in = ParameterIn.PATH,
        required = true,
        description = "The status to update",
        schema = @Schema(implementation = String.class)
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully updated the shipment status",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShippingDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request body or status"
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Shipment with the specified ID not found"
      )
    }
  )
  Mono<ShippingDto> updateShippingStatusByOrderId(@PathVariable int orderId, @RequestBody String status);
}
