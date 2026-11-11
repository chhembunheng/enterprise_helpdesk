package com.stk.ticket_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

import com.stk.ticket_service.dto.TicketResponse;
import com.stk.ticket_service.dto.CreateTicketResponse; 
import com.stk.ticket_service.dto.UpdateTicketResponse;
import com.stk.ticket_service.service.impl.TicketServiceImpl;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketServiceImpl ticketService;

    public TicketController(TicketServiceImpl ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "Ticket Service is running!";
    }

    @GetMapping("")
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        List<TicketResponse> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable("id") String id) {
        TicketResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketResponse createTicketResponse) {
        TicketResponse ticket = ticketService.createTicket(createTicketResponse);
        return ResponseEntity.ok(ticket);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable("id") String id, 
            @Valid @RequestBody UpdateTicketResponse updateTicketResponse) {
        TicketResponse ticket = ticketService.updateTicket(id, updateTicketResponse);
        return ResponseEntity.ok(ticket);
    }

	@PatchMapping ("/{id}/delete")
	public ResponseEntity<Void> deleteTicket(@PathVariable("id") String id) {
		ticketService.deleteTicket(id);
		return ResponseEntity.noContent().build();
	}

    @GetMapping("/search")
    public ResponseEntity<List<TicketResponse>> searchTicket(@RequestParam("title") @NotBlank String title) {
        List<TicketResponse> tickets = ticketService.searchTicket(title);
        return ResponseEntity.ok(tickets);
    }
}
