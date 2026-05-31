package com.example.blindpay.bootstrap;

import com.example.blindpay.config.BlindPayProperties;
import com.example.blindpay.model.User;
import com.example.blindpay.repository.UserRepository;
import com.example.blindpay.service.BlindPayApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BlindPayProperties properties;
    private final BlindPayApiService blindPayApi;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("  BlindPay Test Service — Bootstrap");
        log.info("========================================");

        if (properties.getTosId() == null || properties.getTosId().isBlank()) {
            log.info("No TOS ID configured. Generating TOS acceptance URL...");
            Map<String, Object> tos = blindPayApi.createTosUrl("http://localhost:8080");
            log.info("============================================================");
            log.info("  IMPORTANT: Open this URL in your browser to accept TOS:");
            log.info("  {}", tos.get("url"));
            log.info("  Then set blindpay.tos-id in application.yml and restart.");
            log.info("============================================================");
            return;
        }

        String tosId = properties.getTosId();
        log.info("Using TOS ID: {}", tosId);

        User alice = createUser("Alice", "Doe", "alice@example.com", tosId,
                "Jane Doe", "021000021", "123456789");
        User bob = createUser("Bob", "Smith", "bob@example.com", tosId,
                "Bob Smith", "021000021", "987654321");

        log.info("========================================");
        log.info("  Bootstrap complete!");
        log.info("  Alice: id={}, receiver={}, wallet={}", alice.getId(), alice.getReceiverId(), alice.getWalletId());
        log.info("  Bob:   id={}, receiver={}, wallet={}", bob.getId(), bob.getReceiverId(), bob.getWalletId());
        log.info("========================================");
    }

    private User createUser(String firstName, String lastName, String email,
                            String tosId, String beneficiaryName,
                            String routingNumber, String accountNumber) {
        log.info("--- Setting up user: {} {} ---", firstName, lastName);

        // 1. Create receiver
        log.info("Creating receiver...");
        Map<String, Object> receiver = blindPayApi.createReceiver(
                firstName, lastName, email, tosId);
        String receiverId = (String) receiver.get("id");
        log.info("Receiver created: {}", receiverId);

        // 2. Create bank account
        log.info("Creating bank account...");
        Map<String, Object> bankAccount = blindPayApi.createBankAccount(
                receiverId, beneficiaryName, routingNumber, accountNumber);
        String bankAccountId = (String) bankAccount.get("id");
        log.info("Bank account created: {}", bankAccountId);

        // 3. Create managed wallet (to get an address)
        log.info("Creating managed wallet...");
        Map<String, Object> wallet = blindPayApi.createWallet(receiverId);
        String walletId = (String) wallet.get("id");
        String walletAddress = (String) wallet.get("address");
        log.info("Managed wallet created: id={}, address={}", walletId, walletAddress);

        // 4. Create blockchain wallet using the managed wallet's address
        log.info("Creating blockchain wallet...");
        Map<String, Object> blockchainWallet = blindPayApi.createBlockchainWallet(
                receiverId, walletAddress);
        String blockchainWalletId = (String) blockchainWallet.get("id");
        log.info("Blockchain wallet created: {}", blockchainWalletId);

        // 5. Save to DB
        User user = User.builder()
                .name(firstName + " " + lastName)
                .email(email)
                .receiverId(receiverId)
                .bankAccountId(bankAccountId)
                .blockchainWalletId(blockchainWalletId)
                .walletId(walletId)
                .walletAddress(walletAddress)
                .tosId(tosId)
                .build();

        return userRepository.save(user);
    }
}
