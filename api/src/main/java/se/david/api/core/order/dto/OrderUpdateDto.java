package se.david.api.core.order.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderUpdateDto(@NotBlank String status) {
}
