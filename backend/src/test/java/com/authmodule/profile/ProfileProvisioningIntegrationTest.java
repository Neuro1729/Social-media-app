package com.authmodule.profile;

import com.authmodule.AbstractIntegrationTest;
import com.authmodule.auth.AuthModels;
import com.authmodule.social.Profile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileProvisioningIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    @Test
    void registerCreatesDefaultProfileAndAllowsImmediateAccess() throws Exception {
        String username = "User_" + UUID.randomUUID().toString().substring(0, 8);
        String email = username.toLowerCase() + "@example.com";

        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123",
                                "username", username
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(register.getResponse().getContentAsString());
        UUID userId = UUID.fromString(body.path("user").path("id").asText());

        Long profileCount = tx().execute(status ->
                entityManager.createQuery(
                                "SELECT COUNT(p) FROM Profile p WHERE p.userId = :userId",
                                Long.class
                        )
                        .setParameter("userId", userId)
                        .getSingleResult()
        );
        assertThat(profileCount).isEqualTo(1L);

        Long identifierCount = tx().execute(status ->
                entityManager.createQuery(
                                """
                                        SELECT COUNT(i) FROM LoginIdentifierEntity i
                                        WHERE i.userId = :userId
                                          AND i.type = :type
                                          AND i.active = true
                                        """,
                                Long.class
                        )
                        .setParameter("userId", userId)
                        .setParameter("type", AuthModels.IdentifierType.USERNAME)
                        .getSingleResult()
        );
        assertThat(identifierCount).isEqualTo(1L);

        mockMvc.perform(get("/api/social/profiles/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(get("/api/social/search/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));

        mockMvc.perform(get("/api/social/profiles/{username}", username.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(get("/api/social/profiles/{username}", username)).andExpect(status().isOk());
        mockMvc.perform(get("/api/social/profiles/{username}", username)).andExpect(status().isOk());

        Long stillOne = tx().execute(status ->
                entityManager.createQuery(
                                "SELECT COUNT(p) FROM Profile p WHERE p.userId = :userId",
                                Long.class
                        )
                        .setParameter("userId", userId)
                        .getSingleResult()
        );
        assertThat(stillOne).isEqualTo(1L);
    }

    @Test
    void missingProfileIsRepairedOnGet() throws Exception {
        String username = "repair_" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@example.com";

        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123",
                                "username", username
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        UUID userId = UUID.fromString(
                objectMapper.readTree(register.getResponse().getContentAsString())
                        .path("user").path("id").asText()
        );

        tx().executeWithoutResult(status -> {
            Profile profile = entityManager.find(Profile.class, userId);
            assertThat(profile).isNotNull();
            entityManager.remove(profile);
            entityManager.flush();
        });

        Profile afterDelete = tx().execute(status -> entityManager.find(Profile.class, userId));
        assertThat(afterDelete).isNull();

        mockMvc.perform(get("/api/social/profiles/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        Profile repaired = tx().execute(status -> entityManager.find(Profile.class, userId));
        assertThat(repaired).isNotNull();
    }
}
