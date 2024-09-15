package se.david.api.core.inventory.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto;

import java.util.List;

@Tag(name = "Inventory Service", description = "REST API for inventory stock information.")
public interface InventoryService {
  @GetMapping(
    value = "/inventories",
    produces = "application/json")
  @Operation(
    summary = "Get all inventory stocks",
    description = "Retrieves a list of inventory stock information.",
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the list of composite orders",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = InventoryDto.class, type = "array")
        )
      )
    }
  )
  List<InventoryDto> getInventoryStocks();

  @GetMapping(
    value = "/inventories/{productId}",
    produces = "application/json")
  @Operation(
    summary = "Get inventory stock for a specific product",
    description = "Retrieves the stock information for a specific inventory item identified by the product ID. The response includes details such as the current stock level for the given product.",
    parameters = {
      @Parameter(
        name = "productId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the product for which to retrieve stock information",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the inventory stock information for the specified product",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = InventoryDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Inventory item with the specified product ID not found"
      )
    }
  )
  InventoryDto getInventoryStock(@PathVariable int productId);

  @PostMapping(
    value = "/inventories",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Create new inventory stock",
    description = "Creates a new inventory stock item for a product with the specified details. The request body should contain the product details and the initial stock quantity.",
    responses = {
      @ApiResponse(
        responseCode = "201",
        description = "Successfully created the inventory stock item",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = InventoryDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request body or stock creation details"
      ),
      @ApiResponse(
        responseCode = "409",
        description = "Inventory item already exists for the specified product"
      )
    }
  )
  InventoryDto createInventoryStock(@RequestBody InventoryDto inventoryCreateRequest);

  @DeleteMapping(
    value = "/inventories/{productId}",
    produces = "application/json")
  @Operation(
    summary = "Delete inventory stock for a product",
    description = "Deletes the inventory stock item associated with the specified product ID. The product must exist in the inventory for it to be deleted.",
    parameters = {
      @Parameter(
        name = "productId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the product for which to delete stock information",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully deleted the inventory stock item"
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Inventory item with the specified product ID not found"
      )
    }
  )
  void deleteInventoryStock(@PathVariable int productId);

  @PutMapping(
    value = "/inventories/increaseStock",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Increase stock of inventory",
    description = "Adjusts the stock quantity of the specified inventory items by increasing their quantities. The request body should contain the details of the inventory items to be adjusted.",
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully increased the stock of the inventory item",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = InventoryDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request body or stock adjustment details"
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Inventory item not found"
      )
    }
  )
  InventoryDto increaseStock(@RequestBody InventoryStockAdjustmentRequestDto inventoryIncreaseRequest);

  @PutMapping(
    value = "/inventories/reduceStock",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Reduce stock of inventory",
    description = "Adjusts the stock quantity of the specified inventory items by reducing their quantities. The request body should contain a list of inventory items to be adjusted.",
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully reduced the stock of the inventory items"
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request body or stock adjustment details"
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Inventory item not found"
      )
    }
  )
  void reduceStock(@RequestBody List<InventoryStockAdjustmentRequestDto> inventoryReduceRequests);


}
