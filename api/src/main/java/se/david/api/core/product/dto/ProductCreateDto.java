package se.david.api.core.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductCreateDto(
  @NotBlank String name,
  @Size(max = 500) String description,
  @Positive int price) {
}
