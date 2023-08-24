package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.controller.util.ControllerTestData;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import jdk.jfr.Description;
import lombok.SneakyThrows;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
@AutoConfigureMockMvc
@Testcontainers
public class TransferControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ControllerTestData controllerTestData;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withUsername(Base64.getEncoder().encodeToString("banking".getBytes()))
            .withPassword(Base64.getEncoder().encodeToString("super-safe-pass".getBytes()));

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @SneakyThrows
    @DisplayName("transfer")
    @Test
    void transferTest() {
        Account sourceAccount = controllerTestData.randomAccount();
        Account destinationAccount = controllerTestData.randomAccount(sourceAccount);

        JSONObject transferAccount = controllerTestData.transferAccount(sourceAccount, destinationAccount);
        BankingUserDetails authUser = controllerTestData.getAuthUser(sourceAccount.getUser().getId());

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferAccount.toString())
                        .with(user(authUser)))
                .andExpect(status().isOk());

        assertEquals(transferAccount.getAsNumber("amount"), sourceAccount.getAmount() - accountRepository.findById(sourceAccount.getId()).orElseThrow().getAmount());
        assertEquals(transferAccount.getAsNumber("amount"), accountRepository.findById(destinationAccount.getId()).orElseThrow().getAmount() - destinationAccount.getAmount());
    }
}
