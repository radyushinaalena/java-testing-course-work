package com.skypro.simplebanking.controller.util;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class ControllerTestData {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    public ControllerTestData(
            UserRepository userRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${number.users.database}") int number) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        createUsers(number);
    }

    private void createUsers(int size) {
        List<User> userList = new ArrayList<>();

        while (size > userList.size()) {
            String name = "User_" + (userList.size() + 1);
            String password = "Password_" + (userList.size() + 1);
            int number = 10 * (userList.size() + 1);

            User user = new User();
            user.setUsername(name);
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
            createAccounts(user, number);
            userList.add(user);
        }
    }

    private void createAccounts(User user, int number) {
        user.setAccounts(new ArrayList<>());
        for (
                AccountCurrency currency : AccountCurrency.values()) {
            Account account = new Account();
            account.setUser(user);
            account.setAccountCurrency(currency);
            account.setAmount((long) (currency.ordinal() + number));
            user.getAccounts().add(account);
            accountRepository.save(account);
        }
    }

    public Account randomAccount(Account... accounts) {
        List<Account> accountList = Arrays.stream(accounts).toList();
        List<Account> allAccountList = accountRepository.findAll();
        if (accountList.size() > 0) {
            allAccountList = allAccountList.stream()
                    .filter(e -> !accountList.contains(e))
                    .filter(e -> e.getAccountCurrency().equals(accountList.get(0).getAccountCurrency()))
                    .toList();
        }
        return allAccountList.get(random.nextInt(allAccountList.size()));
    }
    public JSONObject getNewUser() {
        long UserNumber = userRepository.findAll().stream()
                .min((e1, e2) -> e2.getId().compareTo(e1.getId()))
                .orElseThrow()
                .getId()
                + 1L;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", "User_" + UserNumber);
        jsonObject.put("password", "User_" + UserNumber + "_password");
        return jsonObject;
    }

    public JSONObject transferAccount(Account account1, Account account2) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fromAccountId", account1.getId());
        jsonObject.put("toAccountId", account2.getId());
        jsonObject.put("toUserId", account2.getUser().getId());
        jsonObject.put("amount", account1.getAmount() / 2);
        return jsonObject;
    }



    public User getRandomUser() {
        List<User> users = userRepository.findAll();
        return users.get(random.nextInt(users.size()));
    }

    public List<Account> randomAccountCurrency() {
        List<Account> listAccount = new ArrayList<>();
        List<Account> listAccounts = accountRepository.findAll();
        listAccount.add(listAccounts.get(random.nextInt(listAccounts.size())));
        listAccounts = listAccounts
                .stream()
                .filter(e -> !e.equals(listAccount.get(0)))
                .filter(e -> !e.getAccountCurrency().equals(listAccount.get(0).getAccountCurrency()))
                .collect(Collectors.toList());
        listAccount.add(listAccounts.get(random.nextInt(listAccounts.size())));
        return listAccount;
    }

    public BankingUserDetails getAuthUser(long id) {
        User user = userRepository.findById(id).orElseThrow();
        return new BankingUserDetails(user.getId(), user.getUsername(), user.getPassword(), false);
    }
}