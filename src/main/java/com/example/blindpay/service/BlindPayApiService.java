package com.example.blindpay.service;

import com.example.blindpay.config.BlindPayProperties;
import com.example.blindpay.exception.BlindPayApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class BlindPayApiService {

    private final RestClient restClient;
    private final BlindPayProperties properties;
    private final ObjectMapper objectMapper;

    public BlindPayApiService(RestClient blindPayRestClient,
                              BlindPayProperties properties,
                              ObjectMapper objectMapper) {
        this.restClient = blindPayRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    private String instancePath() {
        return "/instances/" + properties.getInstanceId();
    }

    // --- TOS ---

    public Map<String, Object> createTosUrl(String redirectUrl) {
        log.info("=== Creating TOS acceptance URL ===");
        Map<String, Object> body = Map.of(
                "idempotency_key", UUID.randomUUID().toString(),
                "redirect_url", redirectUrl
        );
        return post(instancePath() + "/tos", body);
    }

    // --- Receivers ---

    public Map<String, Object> createReceiver(String firstName, String lastName,
                                               String email, String tosId) {
        log.info("=== Creating receiver: {} {} ===", firstName, lastName);
        Map<String, Object> body = Map.of(
                "first_name", firstName,
                "last_name", lastName,
                "email", email,
                "tos_id", tosId
        );
        return post(instancePath() + "/receivers", body);
    }

    // --- Bank Accounts ---

    public Map<String, Object> createBankAccount(String receiverId,
                                                  String beneficiaryName,
                                                  String routingNumber,
                                                  String accountNumber) {
        log.info("=== Creating ACH bank account for receiver {} ===", receiverId);
        Map<String, Object> body = Map.of(
                "receiver_id", receiverId,
                "payment_rail", "ach",
                "beneficiary_name", beneficiaryName,
                "routing_number", routingNumber,
                "account_number", accountNumber
        );
        return post(instancePath() + "/bank-accounts", body);
    }

    // --- Blockchain Wallets ---

    public Map<String, Object> createBlockchainWallet(String receiverId,
                                                       String walletAddress) {
        log.info("=== Creating blockchain wallet for receiver {} on Base Sepolia ===", receiverId);
        Map<String, Object> body = Map.of(
                "receiver_id", receiverId,
                "address", walletAddress,
                "chain_id", 84532,
                "is_account_abstraction", true
        );
        return post(instancePath() + "/blockchain-wallets", body);
    }

    // --- Managed Wallets ---

    public Map<String, Object> createWallet(String receiverId) {
        log.info("=== Creating managed wallet for receiver {} ===", receiverId);
        Map<String, Object> body = Map.of(
                "receiver_id", receiverId
        );
        return post(instancePath() + "/wallets", body);
    }

    // --- Payin Quotes ---

    public Map<String, Object> createPayinQuote(String walletId, int amount,
                                                 String paymentMethod, String stablecoin) {
        log.info("=== Creating payin quote: amount={}, wallet={} ===", amount, walletId);
        Map<String, Object> body = Map.of(
                "wallet_id", walletId,
                "amount", amount,
                "payment_method", paymentMethod,
                "stablecoin", stablecoin
        );
        return post(instancePath() + "/payin-quotes", body);
    }

    // --- Payins ---

    public Map<String, Object> createPayin(String payinQuoteId) {
        log.info("=== Executing payin with quote {} ===", payinQuoteId);
        Map<String, Object> body = Map.of(
                "payin_quote_id", payinQuoteId
        );
        return post(instancePath() + "/payins/evm", body);
    }

    // --- Payout Quotes ---

    public Map<String, Object> createPayoutQuote(String bankAccountId, int amount) {
        log.info("=== Creating payout quote: amount={}, bankAccount={} ===", amount, bankAccountId);
        Map<String, Object> body = Map.of(
                "bank_account_id", bankAccountId,
                "amount", amount
        );
        return post(instancePath() + "/quotes", body);
    }

    // --- Payouts ---

    public Map<String, Object> createPayout(String quoteId, String senderWalletAddress) {
        log.info("=== Executing payout with quote {}, sender={} ===", quoteId, senderWalletAddress);
        Map<String, Object> body = Map.of(
                "quote_id", quoteId,
                "sender_wallet_address", senderWalletAddress
        );
        return post(instancePath() + "/payouts/evm", body);
    }

    // --- Transfer Quotes ---

    public Map<String, Object> createTransferQuote(String walletId,
                                                    String receiverWalletAddress,
                                                    int amount) {
        log.info("=== Creating transfer quote: amount={}, from wallet={}, to={} ===",
                amount, walletId, receiverWalletAddress);
        Map<String, Object> body = Map.of(
                "wallet_id", walletId,
                "receiver_wallet_address", receiverWalletAddress,
                "amount", amount
        );
        return post(instancePath() + "/transfer-quotes", body);
    }

    // --- Transfers ---

    public Map<String, Object> createTransfer(String transferQuoteId) {
        log.info("=== Executing transfer with quote {} ===", transferQuoteId);
        Map<String, Object> body = Map.of(
                "transfer_quote_id", transferQuoteId
        );
        return post(instancePath() + "/transfers", body);
    }

    // --- Generic POST helper ---

    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("-> POST {} | body: {}", path, requestJson);

            String responseJson = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("<- POST {} | response: {}", path, responseJson);

            Map<String, Object> result = objectMapper.readValue(
                    responseJson, new TypeReference<>() {});
            return result;
        } catch (RestClientResponseException ex) {
            log.error("BlindPay API error on POST {}: {} — {}",
                    path, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BlindPayApiException(
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Error calling BlindPay API POST {}: {}", path, ex.getMessage(), ex);
            throw new RuntimeException("Failed to call BlindPay API: " + path, ex);
        }
    }
}
