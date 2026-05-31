package com.example.blindpay.bootstrap;

import com.example.blindpay.model.User;
import com.example.blindpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping bootstrap.");
            return;
        }

        log.info("BlindPay Test Service — Bootstrap started");

        User alice = userRepository.save(User.builder()
                .name("Alice Doe")
                .email("alice@example.com")
                .receiverId("re_6qxogYN4CPcO")
                .bankAccountId("ba_eDjpSDt9eqle")
                .walletId("bl_OgayfnsAByD3")
                .walletAddress("0x3734cd5c558b785866ed4c2e0757ac5cef0ccbc6")
                .blockchainWalletId("bw_HGrABaPN74zg")
                .tosId("to_jZGTqFxWn30Z")
                .build());

        User bob = userRepository.save(User.builder()
                .name("Bob Smith")
                .email("bob@example.com")
                .receiverId("re_EetWHy6Ry77V")
                .bankAccountId("ba_d6CAyOpCvpTy")
                .walletId("bl_XdhbzARIF6Q2")
                .walletAddress("0xe19beab0417c4ca1cbb4e17c69b80fcc739eb7e1")
                .blockchainWalletId("bw_DzRpvayE0yV0")
                .tosId("to_nHelySbXX4X2")
                .build());

        log.info("Alice: id={}, receiver={}, wallet={}", alice.getId(), alice.getReceiverId(), alice.getWalletId());
        log.info("Bob:   id={}, receiver={}, wallet={}", bob.getId(), bob.getReceiverId(), bob.getWalletId());
        log.info("Bootstrap complete! Service is ready.");
    }
}
