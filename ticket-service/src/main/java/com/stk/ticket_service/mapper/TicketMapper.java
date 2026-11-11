package com.stk.ticket_service.mapper;

import com.stk.ticket_service.dto.TicketResponse;
import com.stk.ticket_service.model.Ticket;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketMapper {
    TicketResponse toResponse(Ticket ticket);
}

