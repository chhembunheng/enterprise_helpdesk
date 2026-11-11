package com.stk.ticket_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.stk.ticket_service.service.TicketService;
import com.stk.ticket_service.dto.CreateTicketResponse;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
@org.springframework.security.test.context.support.WithMockUser(username = "2ecf09ec-42d4-4017-b86c-d57da67602ac")
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
