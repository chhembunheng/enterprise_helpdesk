package com.stk.ticket_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stk.ticket_service.model.Ticket;
import java.util.UUID;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
	List<Ticket> findByTitleContainingIgnoreCase(String title);
    List<Ticket> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);
    List<Ticket> findByTitleContainingIgnoreCaseAndCreatedBy(String title, UUID createdBy);
    java.util.Optional<Ticket> findByIdAndCreatedBy(UUID id, UUID createdBy);
}
