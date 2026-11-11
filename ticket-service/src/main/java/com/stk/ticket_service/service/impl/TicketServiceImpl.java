package com.stk.ticket_service.service.impl;

import com.stk.ticket_service.dto.CreateTicketResponse;
import com.stk.ticket_service.dto.TicketResponse;
import com.stk.ticket_service.dto.UpdateTicketResponse;
import com.stk.ticket_service.mapper.TicketMapper;
import com.stk.ticket_service.model.Ticket;
import com.stk.ticket_service.repository.TicketRepository;
import com.stk.ticket_service.service.TicketService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    public TicketServiceImpl(TicketRepository ticketRepository, TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
    }

    @Override
    public List<TicketResponse> getAllTickets() {
        List<Ticket> tickets = ticketRepository.findByCreatedByOrderByCreatedAtDesc(currentUserId());
        return tickets.stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketResponse createTicketResponse) {
        try {
            Ticket ticket = new Ticket();
            ticket.setTitle(createTicketResponse.title());
            ticket.setDescription(createTicketResponse.description());
            ticket.setStatus("OPEN");
            ticket.setCreatedBy(currentUserId());
            ticket.setUpdatedBy(ticket.getCreatedBy());

            return ticketMapper.toResponse(ticketRepository.save(ticket));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid input: " + e.getMessage());
        }
    }

    @Override
    public TicketResponse getTicketById(String id) {
        try {
            Ticket ticket = ticketRepository.findByIdAndCreatedBy(UUID.fromString(id), currentUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
            return ticketMapper.toResponse(ticket);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ticket ID");
        }
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(String id, UpdateTicketResponse updateTicketResponse) {
        try {
            Ticket ticket = ticketRepository.findByIdAndCreatedBy(UUID.fromString(id), currentUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

            ticket.setUpdatedBy(currentUserId());
            if (updateTicketResponse.title() != null) {
                ticket.setTitle(updateTicketResponse.title());
            }
            if (updateTicketResponse.description() != null) {
                ticket.setDescription(updateTicketResponse.description());
            }
            if (updateTicketResponse.status() != null) {
                ticket.setStatus(updateTicketResponse.status());
            }

            return ticketMapper.toResponse(ticketRepository.save(ticket));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ticket ID");
        }
    }

    @Override
    @Transactional
    public void deleteTicket(String id) {
        try {
            Ticket ticket = ticketRepository.findByIdAndCreatedBy(UUID.fromString(id), currentUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
            ticketRepository.delete(ticket);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ticket ID");
        }
    }

    @Override
    public List<TicketResponse> searchTicket(String title) {
        return ticketRepository.findByTitleContainingIgnoreCaseAndCreatedBy(title.trim(), currentUserId()).stream()
                .map(ticketMapper::toResponse)
                .toList();
    }
    private UUID currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        try { return UUID.fromString(authentication.getName()); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user identity");
        }
    }

}
