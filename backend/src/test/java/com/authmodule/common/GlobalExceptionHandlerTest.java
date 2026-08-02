package com.authmodule.common;

import com.authmodule.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.BoomConfig.class)
class GlobalExceptionHandlerTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void unexpectedExceptionReturnsJson500NotEmpty403() throws Exception {
        mockMvc.perform(get("/api/test/boom").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @TestConfiguration
    static class BoomConfig {
        @RestController
        static class BoomController {
            private final BoomService boomService;

            BoomController(BoomService boomService) {
                this.boomService = boomService;
            }

            @GetMapping("/api/test/boom")
            public String boom() {
                return boomService.explode();
            }
        }

        @Service
        static class BoomService {
            String explode() {
                throw new IllegalStateException("secret internals");
            }
        }
    }
}
