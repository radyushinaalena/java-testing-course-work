package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.controller.util.ControllerTestData;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.User;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
@AutoConfigureMockMvc

public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ControllerTestData controllerTestData;
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
    @DisplayName("createUser")
    @Test
    @Transactional
    void createUserTest(@Value("${app.security.admin-token}") String adminToken) {
        JSONObject newUserJson = controllerTestData.getNewUser();

        mockMvc.perform(post("/user/")
                        .header("X-SECURITY-ADMIN-KEY", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson.toString()))
                .andExpect(status().isOk());

        assertTrue(userRepository.findByUsername(newUserJson.getAsString("username")).isPresent());

    }

    @SneakyThrows
    @DisplayName("getAllUsers")
    @Test
    @WithMockUser
    void getAllUsersTest() {
        mockMvc.perform(get("/user/list"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$").isNotEmpty(),
                        jsonPath("$").isArray());
    }

    @SneakyThrows
    @DisplayName("getMyProfile")
    @Test
    void getMyProfileTest() {
        User randomUser = controllerTestData.getRandomUser();
        BankingUserDetails authUser = controllerTestData.getAuthUser(randomUser.getId());

        mockMvc.perform(get("/user/me")
                        .with(user(authUser)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(randomUser.getId()),
                        jsonPath("$.username").value(randomUser.getUsername())
                );
    }
}
