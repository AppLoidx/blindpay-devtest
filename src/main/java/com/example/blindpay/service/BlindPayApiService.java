package com.example.blindpay.service;

import com.example.blindpay.config.BlindPayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlindPayApiService {

    private static final String NETWORK = "polygon_amoy";
    private static final String NETWORK_KEY = "network";
    private static final String RECEIVERS_PATH = "/receivers/";
    private static final String REQUEST_AMOUNT = "request_amount";

    private final BlindPayProperties properties;
    private final CurlHttpClient http;

    private String instanceUrl() {
        return properties.getBaseUrl() + "/v1/instances/" + properties.getInstanceId();
    }

    private String eInstanceUrl() {
        return properties.getBaseUrl() + "/v1/e/instances/" + properties.getInstanceId();
    }

    private String receiverUrl(String receiverId) {
        return instanceUrl() + RECEIVERS_PATH + receiverId;
    }

    // --- TOS ---

    public Map<String, Object> createTosUrl(String redirectUrl) {
        log.info("=== Creating TOS acceptance URL ===");
        Map<String, Object> body = Map.of(
                "idempotency_key", UUID.randomUUID().toString(),
                "redirect_url", redirectUrl
        );
        return http.post(eInstanceUrl() + "/tos", body);
    }

    // --- Upload (for KYC files) ---

    public String uploadFile(String filePath) {
        log.info("=== Uploading file: {} ===", filePath);
        return http.uploadForm(properties.getBaseUrl() + "/v1/upload", filePath, "onboarding");
    }

    // --- Receivers ---

    public Map<String, Object> createReceiver(String firstName, String lastName,
                                               String email, String tosId,
                                               String selfieFileUrl, String idDocFrontFileUrl) {
        log.info("=== Creating receiver: {} {} ===", firstName, lastName);
        Map<String, Object> body = new HashMap<>();
        body.put("first_name", firstName);
        body.put("last_name", lastName);
        body.put("email", email);
        body.put("type", "individual");
        body.put("kyc_type", "standard");
        body.put("country", "US");
        body.put("tax_id", "123456789");
        body.put("address_line_1", "123 Main St");
        body.put("city", "New York");
        body.put("state_province_region", "NY");
        body.put("postal_code", "10001");
        body.put("date_of_birth", "1990-01-01T00:00:00.000Z");
        body.put("id_doc_country", "US");
        body.put("id_doc_type", "PASSPORT");
        body.put("selfie_file", selfieFileUrl);
        body.put("id_doc_front_file", idDocFrontFileUrl);
        body.put("tos_id", tosId);
        return http.post(instanceUrl() + "/receivers", body);
    }

    public Map<String, Object> getReceiver(String receiverId) {
        log.info("=== Getting receiver {} ===", receiverId);
        return http.get(receiverUrl(receiverId));
    }

    // --- Bank Accounts (PIX for dev simplicity) ---

    public Map<String, Object> createBankAccount(String receiverId,
                                                  String name, String pixKey) {
        log.info("=== Creating PIX bank account for receiver {} ===", receiverId);
        Map<String, Object> body = Map.of(
                "type", "pix",
                "name", name,
                "pix_key", pixKey
        );
        return http.post(receiverUrl(receiverId) + "/bank-accounts", body);
    }

    // --- Blockchain Wallets ---

    public Map<String, Object> createBlockchainWallet(String receiverId,
                                                       String name,
                                                       String walletAddress) {
        log.info("=== Creating blockchain wallet for receiver {} on polygon_amoy ===", receiverId);
        Map<String, Object> body = Map.of(
                "name", name,
                NETWORK_KEY, NETWORK,
                "address", walletAddress,
                "is_account_abstraction", true
        );
        return http.post(receiverUrl(receiverId) + "/blockchain-wallets", body);
    }

    // --- Managed Wallets ---

    public Map<String, Object> createWallet(String receiverId, String name) {
        log.info("=== Creating managed wallet for receiver {} ===", receiverId);
        Map<String, Object> body = Map.of(
                NETWORK_KEY, NETWORK,
                "name", name
        );
        return http.post(receiverUrl(receiverId) + "/wallets", body);
    }

    public Map<String, Object> getWallet(String receiverId, String walletId) {
        log.info("=== Getting wallet {} for receiver {} ===", walletId, receiverId);
        return http.get(receiverUrl(receiverId) + "/wallets/" + walletId);
    }

    public Map<String, Object> getWalletBalance(String receiverId, String walletId) {
        log.info("=== Getting wallet balance {} for receiver {} ===", walletId, receiverId);
        return http.get(receiverUrl(receiverId) + "/wallets/" + walletId + "/balance");
    }

    // --- Payin Quotes ---

    public Map<String, Object> createPayinQuote(String walletId, int amount) {
        log.info("=== Creating payin quote: amount={}, wallet={} ===", amount, walletId);
        Map<String, Object> body = Map.of(
                "wallet_id", walletId,
                REQUEST_AMOUNT, amount,
                "currency_type", "receiver",
                "payment_method", "pix",
                "token", "USDB"
        );
        return http.post(instanceUrl() + "/payin-quotes", body);
    }

    // --- Payins ---

    public Map<String, Object> createPayin(String payinQuoteId) {
        log.info("=== Executing payin with quote {} ===", payinQuoteId);
        Map<String, Object> body = Map.of(
                "payin_quote_id", payinQuoteId
        );
        return http.post(instanceUrl() + "/payins/evm", body);
    }

    // --- Payout Quotes ---

    public Map<String, Object> createPayoutQuote(String bankAccountId, int amount) {
        log.info("=== Creating payout quote: amount={}, bankAccount={} ===", amount, bankAccountId);
        Map<String, Object> body = Map.of(
                "bank_account_id", bankAccountId,
                REQUEST_AMOUNT, amount,
                "currency_type", "sender",
                NETWORK_KEY, NETWORK,
                "token", "USDB"
        );
        return http.post(instanceUrl() + "/quotes", body);
    }

    // --- Payouts ---

    public Map<String, Object> createPayout(String quoteId, String senderWalletAddress) {
        log.info("=== Executing payout with quote {}, sender={} ===", quoteId, senderWalletAddress);
        Map<String, Object> body = Map.of(
                "quote_id", quoteId,
                "sender_wallet_address", senderWalletAddress
        );
        return http.post(instanceUrl() + "/payouts/evm", body);
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
                REQUEST_AMOUNT, amount,
                "amount_reference", "sender",
                "sender_token", "USDB",
                "receiver_token", "USDB",
                "receiver_network", NETWORK
        );
        return http.post(instanceUrl() + "/transfer-quotes", body);
    }

    // --- Transfers ---

    public Map<String, Object> createTransfer(String transferQuoteId) {
        log.info("=== Executing transfer with quote {} ===", transferQuoteId);
        Map<String, Object> body = Map.of(
                "transfer_quote_id", transferQuoteId
        );
        return http.post(instanceUrl() + "/transfers", body);
    }
}
