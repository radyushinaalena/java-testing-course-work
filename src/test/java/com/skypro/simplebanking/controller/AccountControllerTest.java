package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.controller.util.ControllerTestData;
import com.skypro.simplebanking.dto.BalanceChangeRequest;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import jdk.jfr.Description;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ControllerTestData controllerTestData;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    ObjectMapper objectMapper;

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
    @DisplayName("getUserAccount")
    @Test
    void getUserAccountTest() {
        Account randomAccount = controllerTestData.randomAccount();
        BankingUserDetails authUser = controllerTestData.getAuthUser(randomAccount.getUser().getId());

        mockMvc.perform(get("/account/{id}", randomAccount.getId())
                        .with(user(authUser)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(randomAccount.getId()),
                        jsonPath("$.amount").value(randomAccount.getAmount()),
                        jsonPath("$.currency").value(randomAccount.getAccountCurrency().name())
                );
    }

    @SneakyThrows
    @DisplayName("depositToAccount")
    @Test
    void depositToAccountTest() {
        Account randomAccount = controllerTestData.randomAccount();
        BankingUserDetails authUser = controllerTestData.getAuthUser(randomAccount.getUser().getId());
        long depositAmount = 10L;
        long expectedAmount = randomAccount.getAmount() + depositAmount;

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(depositAmount);
        String jsonBalance = objectMapper.writeValueAsString(balanceChangeRequest);


        mockMvc.perform(post("/account/deposit/{id}", randomAccount.getId())
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBalance))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(randomAccount.getId()),
                        jsonPath("$.amount").value(expectedAmount),
                        jsonPath("$.currency").value(randomAccount.getAccountCurrency().name()));

        long actualAmount = accountRepository.findById(randomAccount.getId()).orElseThrow().getAmount();
        assertEquals(expectedAmount, actualAmount);
    }

    @SneakyThrows
    @DisplayName("withdrawFromAccount")
    @Test
    void withdrawFromAccountTest() {
        Account randomAccount = controllerTestData.randomAccount();
        BankingUserDetails authUser = controllerTestData.getAuthUser(randomAccount.getUser().getId());
        long withdrawAmount = 10L;
        long expectedAmount = randomAccount.getAmount() - withdrawAmount;

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(withdrawAmount);
        String jsonBalance = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/withdraw/{id}", randomAccount.getId())
                        .with(user(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBalance))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(randomAccount.getId()),
                        jsonPath("$.amount").value(expectedAmount),
                        jsonPath("$.currency").value(randomAccount.getAccountCurrency().name()));

        long actualAmount = accountRepository.findById(randomAccount.getId()).orElseThrow().getAmount();
        assertEquals(expectedAmount, actualAmount);
    }
}