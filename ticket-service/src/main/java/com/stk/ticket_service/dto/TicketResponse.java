package com.stk.ticket_service.dto;

public record TicketResponse(
	String id,
	String title,
	String description,
	String status
) {}
