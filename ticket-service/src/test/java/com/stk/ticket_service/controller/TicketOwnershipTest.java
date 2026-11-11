package com.stk.ticket_service.controller;

import com.jayway.jsonpath.JsonPath;
import com.stk.ticket_service.model.Ticket;
import com.stk.ticket_service.repository.TicketRepository;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureMockMvc
@Transactional
class TicketOwnershipTest {
    @Autowired MockMvc mvc;
    @Autowired TicketRepository repository;
    @Autowired SecretKey key;
    @Value("${helpdesk.jwt.issuer}") String issuer;
    @Value("${helpdesk.jwt.audience}") String audience;
    private static final UUID OWNER = UUID.fromString("7d22c739-6e61-41c7-82e4-5216b4229231");
    private static final UUID OTHER = UUID.fromString("d10e4227-531b-43d4-a137-5f7931a73816");

    private Ticket ticket(UUID owner, String title) {
        Ticket ticket = new Ticket(); ticket.setTitle(title); ticket.setCreatedBy(owner);
        return repository.saveAndFlush(ticket);
    }
    @Test
    void rejectsUnauthenticatedSubmission() throws Exception {
        mvc.perform(post("/tickets").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Help please\"}"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void submissionUsesTheVerifiedUserAndForcesOpenStatus() throws Exception {
        String body = "{\"title\":\"Help please\",\"description\":\"Cannot log in\",\"status\":\"CLOSED\",\"createdBy\":\"" + OTHER + "\"}";
        String response = mvc.perform(post("/tickets").with(jwt().jwt(j -> j.subject(OWNER.toString())))
                .header("User-ID", OTHER.toString()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");
        Ticket saved = repository.findById(UUID.fromString(id)).orElseThrow();
        assertEquals(OWNER, saved.getCreatedBy()); assertEquals(OWNER, saved.getUpdatedBy());
    }
    @Test
    void listsAndSearchesOnlyTheCurrentUsersTickets() throws Exception {
        String keyword = "Keyword" + UUID.randomUUID().toString().replace("-", "");
        ticket(OWNER, keyword); ticket(OTHER, keyword); ticket(null, keyword);
        mvc.perform(get("/tickets").with(jwt().jwt(j -> j.subject(OWNER.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/tickets/search").param("title", keyword).with(jwt().jwt(j -> j.subject(OWNER.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }
    @Test
    void hidesOtherUsersTicketsOnReadUpdateAndDelete() throws Exception {
        Ticket other = ticket(OTHER, "Private request");
        mvc.perform(get("/tickets/" + other.getId()).with(jwt().jwt(j -> j.subject(OWNER.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/tickets/" + other.getId()).with(jwt().jwt(j -> j.subject(OWNER.toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Changed\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/tickets/" + other.getId() + "/delete").with(jwt().jwt(j -> j.subject(OWNER.toString()))))
                .andExpect(status().isNotFound());
        assertEquals("Private request", repository.findById(other.getId()).orElseThrow().getTitle());
    }
    @Test
    void canReadUpdateAndDeleteAnOwnedTicket() throws Exception {
        Ticket own = ticket(OWNER, "My request");
        mvc.perform(get("/tickets/" + own.getId()).with(jwt().jwt(j -> j.subject(OWNER.toString()))))
                .andExpect(status().isOk());
        mvc.perform(patch("/tickets/" + own.getId()).with(jwt().jwt(j -> j.subject(OWNER.toString())))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Updated request\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated request"));
        mvc.perform(patch("/tickets/" + own.getId() + "/delete").with(jwt().jwt(j -> j.subject(OWNER.toString()))))
                .andExpect(status().isNoContent());
        assertFalse(repository.existsById(own.getId()));
    }
    private String token(String issuer, String audience, String subject, Instant expires) {
        var claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                .subject(subject).issuedAt(Instant.now().minusSeconds(600));
        if (expires != null) claims.expiresAt(expires);
        return NimbusJwtEncoder.withSecretKey(key).build().encode(JwtEncoderParameters.from(JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }
    @Test
    void validatesSignedTokenIssuerAudienceExpiryAndIdentity() throws Exception {
        Instant future = Instant.now().plusSeconds(600);
        String valid = token(issuer, audience, OWNER.toString(), future);
        mvc.perform(get("/tickets").header("Authorization", "Bearer " + valid)).andExpect(status().isOk());
        for (String invalid : List.of(
                token("wrong-issuer", audience, OWNER.toString(), future),
                token(issuer, "wrong-audience", OWNER.toString(), future),
                token(issuer, audience, OWNER.toString(), Instant.now().minusSeconds(300)),
                token(issuer, audience, "invalid-identity", future),
                token(issuer, audience, OWNER.toString(), null), "invalid-token")) {
            mvc.perform(get("/tickets").header("Authorization", "Bearer " + invalid)).andExpect(status().isUnauthorized());
        }
    }
}
