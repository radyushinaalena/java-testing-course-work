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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AccountControllerNegativeTest {

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
    @DisplayName("getUserAccountNotFound")
    @Test
    void getUserAccountNotFoundTest() {
        Account account = controllerTestData.randomAccount();
        Account account1 = controllerTestData.randomAccount(account);
        account.setUser(account1.getUser());

        BankingUserDetails authUser = controllerTestData.getAuthUser(account.getUser().getId());

        mockMvc.perform(get("/account/{id}", account.getId()).with(user(authUser))).andExpect(status().isNotFound());
    }

    @SneakyThrows
    @DisplayName("depositToAccountBadRequest")
    @Test
    void depositToAccountBadRequestTest() {
        Account randomAccount = controllerTestData.randomAccount();
        BankingUserDetails authUser = controllerTestData.getAuthUser(randomAccount.getUser().getId());
        long depositAmount = -10L;

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(depositAmount);
        String jsonBalance = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/deposit/{id}", randomAccount.getId()).with(user(authUser)).contentType(MediaType.APPLICATION_JSON).content(jsonBalance)).andExpectAll(status().isBadRequest(), jsonPath("$").value("Amount should be more than 0"));
    }

    @SneakyThrows
    @DisplayName("depositToAccountNotFound")
    @Test
    void depositToAccountNotFoundTest() {
        Account account = controllerTestData.randomAccount();
        Account account1 = controllerTestData.randomAccount(account);

        BankingUserDetails authUser = controllerTestData.getAuthUser(account.getUser().getId());
        long depositAmount = 10L;

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(depositAmount);
        String jsonBalance = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/deposit/{id}", account1.getId()).with(user(authUser)).contentType(MediaType.APPLICATION_JSON).content(jsonBalance)).andExpect(status().isNotFound());
    }

    @SneakyThrows
    @DisplayName("withdrawFromAccountBadRequest")
    @Test
    void withdrawFromAccountBadRequestTest() {
        Account randomAccount = controllerTestData.randomAccount();
        BankingUserDetails authUser = controllerTestData.getAuthUser(randomAccount.getUser().getId());
        long withdrawAmount = -10L;

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(withdrawAmount);
        String jsonBalance = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/withdraw/{id}", randomAccount.getId()).with(user(authUser)).contentType(MediaType.APPLICATION_JSON).content(jsonBalance)).andExpectAll(status().isBadRequest(), jsonPath("$").value("Amount should be more than 0"));
    }

    @SneakyThrows
    @DisplayName("withdrawFromAccountNotFound")
    @Test
    void withdrawFromAccountNotFoundTest() {
        Account account = controllerTestData.randomAccount();
        Account account1 = controllerTestData.randomAccount(account);

        BankingUserDetails authUser = controllerTestData.getAuthUser(account.getUser().getId());
        long withdrawAmount = 10L;

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(withdrawAmount);
        String jsonBalance = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/withdraw/{id}", account1.getId()).with(user(authUser)).contentType(MediaType.APPLICATION_JSON).content(jsonBalance)).andExpect(status().isNotFound());
    }
}