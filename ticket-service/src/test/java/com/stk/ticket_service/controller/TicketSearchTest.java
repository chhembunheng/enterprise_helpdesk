package com.stk.ticket_service.controller;

import com.stk.ticket_service.model.Ticket;
import com.stk.ticket_service.repository.TicketRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureMockMvc
@Transactional
class TicketSearchTest {
    private static final UUID USER_ID = UUID.fromString("2ecf09ec-42d4-4017-b86c-d57da67602ac");
    @Autowired
    private MockMvc mvc;

    @Autowired
    private TicketRepository repository;

    private void saveTicket(String title) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setCreatedBy(USER_ID);
        repository.saveAndFlush(ticket);
    }

    @Test
    void searchesPartOfTheTitleIgnoringCaseAndTrimsTheQuery() throws Exception {
        String keyword = UUID.randomUUID().toString();
        String first = "Learn " + keyword.toUpperCase() + " Spring Boot";
        String second = "Advanced " + keyword + " Java";
        saveTicket(first);
        saveTicket(second);
        saveTicket("Unrelated " + UUID.randomUUID());

        mvc.perform(get("/tickets/search").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(USER_ID.toString()))).param("title", "  " + keyword + "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder(first, second)));
    }

    @Test
    void returnsAnEmptyListWhenNoTitleMatches() throws Exception {
        mvc.perform(get("/tickets/search").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(USER_ID.toString()))).param("title", "missing-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void supportsMultipleTicketsWithTheSameTitle() throws Exception {
        String title = "Learn " + UUID.randomUUID();
        saveTicket(title);
        saveTicket(title);

        mvc.perform(get("/tickets/search").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(USER_ID.toString()))).param("title", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsBlankTitles(String title) throws Exception {
        mvc.perform(get("/tickets/search").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(USER_ID.toString()))).param("title", title))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresTheTitleParameter() throws Exception {
        mvc.perform(get("/tickets/search").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isBadRequest());
    }
}
