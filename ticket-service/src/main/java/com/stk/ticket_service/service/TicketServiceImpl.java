package com.stk.ticket_service.service;

import com.stk.ticket_service.dto.CreateTicketResponse;
import com.stk.ticket_service.dto.TicketResponse;
import com.stk.ticket_service.dto.UpdateTicketResponse;
import com.stk.ticket_service.model.Ticket;
import com.stk.ticket_service.repository.TicketRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    public TicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public List<TicketResponse> getAllTickets() {
        List<Ticket> tickets = ticketRepository.findAll();
        return tickets.stream()
                .map(TicketResponse::fromTicket)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketResponse createTicketResponse) {
        try {
            Ticket ticket = new Ticket();
            ticket.setTitle(createTicketResponse.title());
            ticket.setDescription(createTicketResponse.description());
            if (createTicketResponse.status() != null && !createTicketResponse.status().isBlank()) {
                ticket.setStatus(createTicketResponse.status());
            }

            return TicketResponse.fromTicket(ticketRepository.save(ticket));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid input: " + e.getMessage());
        }
    }

    @Override
    public TicketResponse getTicketById(String id) {
        try {
            Ticket ticket = ticketRepository.findById(UUID.fromString(id))
                    .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));
            return TicketResponse.fromTicket(ticket);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid UUID format: " + id);
        }
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(String id, UpdateTicketResponse updateTicketResponse) {
        try {
            Ticket ticket = ticketRepository.findById(UUID.fromString(id))
                    .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));

            if (updateTicketResponse.title() != null) {
                ticket.setTitle(updateTicketResponse.title());
            }
            if (updateTicketResponse.description() != null) {
                ticket.setDescription(updateTicketResponse.description());
            }
            if (updateTicketResponse.status() != null) {
                ticket.setStatus(updateTicketResponse.status());
            }

            return TicketResponse.fromTicket(ticketRepository.save(ticket));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid UUID format: " + id);
        }
    }

    @Override
    @Transactional
    public void deleteTicket(String id) {
        try {
            Ticket ticket = ticketRepository.findById(UUID.fromString(id))
                    .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));
            ticketRepository.delete(ticket);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid UUID format: " + id);
        }
    }
}
