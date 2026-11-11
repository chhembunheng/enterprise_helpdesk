package com.stk.ticket_service.mapper;

import com.stk.ticket_service.dto.TicketResponse;
import com.stk.ticket_service.model.Ticket;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TicketMapperTest {
    private final TicketMapper mapper = Mappers.getMapper(TicketMapper.class);

    @Test
    void mapsTicketToTheExistingResponseFormat() {
        Ticket ticket = new Ticket();
        UUID id = UUID.fromString("c8148205-19e7-4ce2-b2c6-23568bcc01e9");
        ticket.setId(id);
        ticket.setTitle("Printer issue");
        ticket.setDescription("Printer is offline");
        ticket.setStatus("OPEN");
        ticket.setPriority("HIGH");

        TicketResponse response = mapper.toResponse(ticket);

        assertEquals(new TicketResponse(id.toString(), "Printer issue", "Printer is offline", "OPEN"), response);
    }

    @Test
    void mapsNullableDescriptionWithoutLosingOtherFields() {
        Ticket ticket = new Ticket();
        UUID id = UUID.fromString("42a9ebef-5f86-448c-80a3-de55746b6b8b");
        ticket.setId(id);
        ticket.setTitle("Login issue");
        ticket.setStatus("CLOSED");

        TicketResponse response = mapper.toResponse(ticket);

        assertEquals(id.toString(), response.id());
        assertEquals("Login issue", response.title());
        assertEquals("CLOSED", response.status());
        assertNull(response.description());
    }
}
