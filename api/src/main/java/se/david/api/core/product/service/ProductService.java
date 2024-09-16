package se.david.api.core.product.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import se.david.api.core.product.dto.ProductDto;

import java.util.List;

@Tag(name = "Product Service", description = "REST API for managing product information.")
public interface ProductService {
  @GetMapping(
    value = "/products",
    produces = "application/json")
  @Operation(
    summary = "Get all products",
    description = "Retrieves a list of all products.",
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the list of products",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ProductDto.class, type = "array")
        )
      )
    }
  )
  List<ProductDto> getProducts();

  @GetMapping(
    value = "/products/byIds",
    produces = "application/json")
  @Operation(
    summary = "Get products by IDs",
    description = "Retrieves product details for a list of product IDs.",
    parameters = {
      @Parameter(
        name = "ids",
        in = ParameterIn.QUERY,
        required = true,
        description = "List of product IDs to retrieve",
        schema = @Schema(type = "array", implementation = Integer.class)
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the products for the specified IDs",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ProductDto.class, type = "array")
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "One or more products not found for the specified IDs"
      )
    }
  )
  List<ProductDto> getProductsByIds(@RequestParam List<Integer> ids);

  @GetMapping(
    value = "/products/{productId}",
    produces = "application/json")
  @Operation(
    summary = "Get product by ID",
    description = "Retrieves the product information for a specific product identified by the product ID.",
    parameters = {
      @Parameter(
        name = "productId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the product to retrieve",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the product information",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ProductDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Product with the specified ID not found"
      )
    }
  )
  ProductDto getProduct(@PathVariable int productId);

  @PostMapping(
    value = "/products",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Create new product",
    description = "Creates a new product with the specified details. The request body should contain the product's details.",
    responses = {
      @ApiResponse(
        responseCode = "201",
        description = "Successfully created the product",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ProductDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request body or product details"
      ),
      @ApiResponse(
        responseCode = "409",
        description = "Product already exists with the specified ID"
      )
    }
  )
  ProductDto createProduct(@RequestBody ProductDto product);

  @PutMapping(
    value = "/products/{productId}",
    consumes = "application/json",
    produces = "application/json")
  @Operation(
    summary = "Update product by ID",
    description = "Updates the product details for a specific product identified by the product ID.",
    parameters = {
      @Parameter(
        name = "productId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the product to update",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully updated the product",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ProductDto.class)
        )
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Invalid request body or product details"
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Product with the specified ID not found"
      )
    }
  )
  ProductDto updateProduct(@PathVariable int productId, @RequestBody ProductDto product);

  @DeleteMapping(
    value = "/products/{productId}")
  @Operation(
    summary = "Delete product by ID",
    description = "Deletes the product associated with the specified product ID.",
    parameters = {
      @Parameter(
        name = "productId",
        in = ParameterIn.PATH,
        required = true,
        description = "The ID of the product to delete",
        schema = @Schema(type = "integer")
      )
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Successfully deleted the product"
      ),
      @ApiResponse(
        responseCode = "404",
        description = "Product with the specified ID not found"
      )
    }
  )
  void deleteProduct(@PathVariable int productId);
}
