package se.david.api.core.order.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderUpdateDto;

@Tag(name = "Order Service", description = "REST API for managing orders.")
public interface OrderService {

  @GetMapping(
    value = "/orders",
    produces = "application/json")
  @Operation(
    summary = "Get all orders",
    description = "Retrieves a list of all orders.",
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the list of orders",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = OrderDto.class, type = "array")
        )
      )
    }
  )
  Flux<OrderDto> getOrders();

  @GetMapping(
    value = "/orders/user/{userId}",
    produces = "application/json")
  @Operation(
    summary = "Get orders for a specific user",
    description = "Retrieves the list of orders for a specific user.",
    parameters = {
      @Parameter(
        name = "userId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the user whose orders are to be retrieved",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the list of orders for the user",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = OrderDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "User not found"
      )
    }
  )
  Flux<OrderDto> getOrdersByUser(@PathVariable int userId);

  @GetMapping(
    value = "/orders/{orderId}",
    produces = "application/json")
  @Operation(
    summary = "Get an order by ID",
    description = "Retrieves a specific order by its ID.",
    parameters = {
      @Parameter(
        name = "orderId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the order to retrieve",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the order",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = OrderDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Order not found"
      )
    }
  )
  Mono<OrderDto> getOrder(@PathVariable int orderId);

  @PostMapping(
    value = "/orders",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Create a new order",
    description = "Creates a new order with the specified details.",
    responses = {
      @ApiResponse(
        responseCode = "201",
        description = "Successfully created the order",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = OrderDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request body"
      )
    }
  )
  Mono<OrderDto> createOrder(@RequestBody OrderCreateDto orderCreateDto);

  @PutMapping(
    value = "/orders/{orderId}",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Update an existing order",
    description = "Updates the details of an existing order by ID.",
    parameters = {
      @Parameter(
        name = "orderId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the order to update",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully updated the order",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = OrderDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request body"
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Order not found"
      )
    }
  )
  Mono<OrderDto> updateOrder(@PathVariable int orderId, @RequestBody OrderUpdateDto orderUpdateDto);

  @DeleteMapping(
    value = "/orders/{orderId}")
  @Operation(
    summary = "Delete an order",
    description = "Deletes the order with the specified ID.",
    parameters = {
      @Parameter(
        name = "orderId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the order to delete",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully deleted the order"
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Order not found"
      )
    }
  )
  Mono<Void> deleteOrder(@PathVariable int orderId);
}