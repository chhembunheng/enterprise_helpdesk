package com.stk.ticket_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.stk.ticket_service.service.TicketService;
import com.stk.ticket_service.dto.CreateTicketResponse;

@SpringBootTest
class TicketServiceApplicationTests {

    @Autowired
    private TicketService ticketService;

	@Test
	void contextLoads() {
	}

    @Test
    void testCreateTicket() {
        ticketService.createTicket(new CreateTicketResponse("Test Title", "Test Description", "OPEN"));
    }

}
