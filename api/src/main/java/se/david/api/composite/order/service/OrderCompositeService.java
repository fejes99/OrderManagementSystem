package se.david.api.composite.order.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.david.api.composite.order.dto.OrderAggregateDto;

import java.util.List;

@Tag(name = "Order Composite Service", description = "REST API for composite order information.")
public interface OrderCompositeService {
  @GetMapping(
    value = "/order-composite",
    produces = "application/json")
  @Operation(
    summary = "Get all composite orders",
    description = "Retrieves a list of composite order information.",
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the list of composite orders",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = OrderAggregateDto.class, type = "array")
        )
      )
    }
  )
  List<OrderAggregateDto> getCompositeOrders();

  @GetMapping(
    value = "/order-composite/user/{userId}",
    produces = "application/json")
  @Operation(
    summary = "Get composite orders by user ID",
    description = "Retrieves a list of composite orders specific to the user based on user ID.",
    parameters = @Parameter(name = "userId", description = "ID of the user to fetch composite orders for", required = true, schema = @Schema(type = "integer")),
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the list of composite orders for the specified user",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = OrderAggregateDto.class, type = "array")
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "User with the specified ID not found"
      )
    }
  )
  List<OrderAggregateDto> getCompositeOrdersByUser(@PathVariable int userId);

  @GetMapping(
    value = "/order-composite/{orderId}",
    produces = "application/json")
  @Operation(
    summary = "Get composite order by order ID",
    description = "Retrieves detailed composite order information based on the provided order ID.",
    parameters = @Parameter(name = "orderId", description = "ID of the order to retrieve", required = true, schema = @Schema(type = "integer")),
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the composite order for the specified ID",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = OrderAggregateDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Order with the specified ID not found"
      )
    }
  )
  OrderAggregateDto getCompositeOrder(@PathVariable int orderId);


  @PostMapping(
    value = "/order-composite",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Create a new composite order",
    description = "Creates a new composite order based on the provided order details.",
    responses = {
      @ApiResponse(
        responseCode = "201",
        description = "Successfully created the composite order",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = OrderAggregateDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid input data"
      )
    }
  )
  OrderAggregateDto createCompositeOrder(@RequestBody OrderAggregateDto orderAggregateDto);
}
