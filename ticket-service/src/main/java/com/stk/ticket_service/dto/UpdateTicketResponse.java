package com.stk.ticket_service.dto;

public record UpdateTicketResponse(
	String id,
	String title,
	String description,
	String status
) {
}
