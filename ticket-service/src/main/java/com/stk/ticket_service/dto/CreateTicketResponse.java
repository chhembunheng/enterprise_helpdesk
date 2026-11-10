package com.stk.ticket_service.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateTicketResponse(
	@NotBlank @Size(max = 100) @Pattern(regexp = "^[a-zA-Z0-9\\s]+$", message = "Title can only contain letters, numbers, and spaces") String title,
	@Size(max = 255) String description,
	String status
) {
}
