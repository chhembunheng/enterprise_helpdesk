package com.stk.ticket_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stk.ticket_service.model.Ticket;
import java.util.UUID;

@Repository

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

}
