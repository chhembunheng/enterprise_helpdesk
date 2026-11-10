package com.stk.ticket_service.service;

import com.stk.ticket_service.dto.CreateTicketResponse;
import com.stk.ticket_service.dto.TicketResponse;
import com.stk.ticket_service.dto.UpdateTicketResponse;
import java.util.List;

public interface TicketService {
	List<TicketResponse> getAllTickets();

	TicketResponse createTicket(CreateTicketResponse createTicketResponse);

	TicketResponse getTicketById(String id);

	TicketResponse updateTicket(String id, UpdateTicketResponse updateTicketResponse);

	void deleteTicket(String id);
}
