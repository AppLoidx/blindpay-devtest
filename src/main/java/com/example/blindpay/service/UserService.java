package com.example.blindpay.service;

import com.example.blindpay.model.User;
import com.example.blindpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BlindPayApiService blindPayApi;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public Map<String, Object> payin(Long userId, int amount) {
        User user = getUser(userId);
        log.info("=== PAYIN: user={} ({}), amount={} ===", user.getName(), userId, amount);

        log.info("Step 1: Creating payin quote...");
        Map<String, Object> quote = blindPayApi.createPayinQuote(
                user.getWalletId(), amount);
        String quoteId = (String) quote.get("id");
        log.info("Payin quote created: {}", quoteId);

        log.info("Step 2: Executing payin...");
        Map<String, Object> payin = blindPayApi.createPayin(quoteId);
        log.info("Payin executed: {}", payin);

        return payin;
    }

    public Map<String, Object> payout(Long userId, int amount) {
        User user = getUser(userId);
        log.info("=== PAYOUT: user={} ({}), amount={} ===", user.getName(), userId, amount);

        log.info("Step 1: Creating payout quote...");
        Map<String, Object> quote = blindPayApi.createPayoutQuote(
                user.getBankAccountId(), amount);
        String quoteId = (String) quote.get("id");
        log.info("Payout quote created: {}", quoteId);

        log.info("Step 2: Executing payout...");
        Map<String, Object> payout = blindPayApi.createPayout(
                quoteId, user.getWalletAddress());
        log.info("Payout executed: {}", payout);

        return payout;
    }

    public Map<String, Object> getBalance(Long userId) {
        User user = getUser(userId);
        log.info("=== BALANCE: user={} ({}) ===", user.getName(), userId);
        return blindPayApi.getWalletBalance(user.getReceiverId(), user.getWalletId());
    }

    public Map<String, Object> transfer(Long fromUserId, Long toUserId, int amount) {
        User fromUser = getUser(fromUserId);
        User toUser = getUser(toUserId);
        log.info("=== TRANSFER: from={} to={}, amount={} ===",
                fromUser.getName(), toUser.getName(), amount);

        log.info("Step 1: Creating transfer quote...");
        Map<String, Object> quote = blindPayApi.createTransferQuote(
                fromUser.getWalletId(), toUser.getWalletAddress(), amount);
        String quoteId = (String) quote.get("id");
        log.info("Transfer quote created: {}", quoteId);

        log.info("Step 2: Executing transfer...");
        Map<String, Object> transfer = blindPayApi.createTransfer(quoteId);
        log.info("Transfer executed: {}", transfer);

        return transfer;
    }
}
