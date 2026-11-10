package com.stk.ticket_service.dto;

import com.stk.ticket_service.model.Ticket;

public record TicketResponse(
	String id,
	String title,
	String description,
	String status
) {
	public static TicketResponse fromTicket(Ticket ticket) {
		return new TicketResponse(
				ticket.getId().toString(),
				ticket.getTitle(),
				ticket.getDescription(),
				ticket.getStatus()
		);
	}
}
