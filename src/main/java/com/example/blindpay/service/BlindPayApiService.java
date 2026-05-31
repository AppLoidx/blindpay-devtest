package com.example.blindpay.service;

import com.example.blindpay.config.BlindPayProperties;
import com.example.blindpay.exception.BlindPayApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BlindPayApiService {

    private static final String NETWORK = "polygon_amoy";
    private static final String NETWORK_KEY = "network";
    private static final String RECEIVERS_PATH = "/receivers/";
    private static final String REQUEST_AMOUNT = "request_amount";
    private static final String AUTH_HEADER_PREFIX = "Authorization: Bearer ";
    private static final String CURL_STATUS_SUFFIX = "\n%{http_code}";

    private final BlindPayProperties properties;
    private final ObjectMapper objectMapper;

    public BlindPayApiService(BlindPayProperties properties,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    private String instancePath() {
        return "/v1/instances/" + properties.getInstanceId();
    }

    private String eInstancePath() {
        return "/v1/e/instances/" + properties.getInstanceId();
    }

    private String baseUrl() {
        return properties.getBaseUrl();
    }

    // --- TOS ---

    public Map<String, Object> createTosUrl(String redirectUrl) {
        log.info("=== Creating TOS acceptance URL ===");
        Map<String, Object> body = Map.of(
                "idempotency_key", UUID.randomUUID().toString(),
                "redirect_url", redirectUrl
        );
        return post(eInstancePath() + "/tos", body);
    }

    // --- Upload (for KYC files) ---

    public String uploadFile(String filePath) {
        log.info("=== Uploading file: {} ===", filePath);
        return uploadForm("/v1/upload", filePath, "onboarding");
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
        return post(instancePath() + "/receivers", body);
    }

    public Map<String, Object> getReceiver(String receiverId) {
        log.info("=== Getting receiver {} ===", receiverId);
        return get(instancePath() + RECEIVERS_PATH + receiverId);
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
        return post(instancePath() + RECEIVERS_PATH + receiverId + "/bank-accounts", body);
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
        return post(instancePath() + RECEIVERS_PATH + receiverId + "/blockchain-wallets", body);
    }

    // --- Managed Wallets ---

    public Map<String, Object> createWallet(String receiverId, String name) {
        log.info("=== Creating managed wallet for receiver {} ===", receiverId);
        Map<String, Object> body = Map.of(
                NETWORK_KEY, NETWORK,
                "name", name
        );
        return post(instancePath() + RECEIVERS_PATH + receiverId + "/wallets", body);
    }

    public Map<String, Object> getWallet(String receiverId, String walletId) {
        log.info("=== Getting wallet {} for receiver {} ===", walletId, receiverId);
        return get(instancePath() + RECEIVERS_PATH + receiverId + "/wallets/" + walletId);
    }

    public Map<String, Object> getWalletBalance(String receiverId, String walletId) {
        log.info("=== Getting wallet balance {} for receiver {} ===", walletId, receiverId);
        return get(instancePath() + RECEIVERS_PATH + receiverId + "/wallets/" + walletId + "/balance");
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
                REQUEST_AMOUNT, amount,
                "currency_type", "sender",
                NETWORK_KEY, NETWORK,
                "token", "USDB"
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
                REQUEST_AMOUNT, amount,
                "amount_reference", "sender",
                "sender_token", "USDB",
                "receiver_token", "USDB",
                "receiver_network", NETWORK
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

    // --- HTTP helpers using curl (Cloudflare blocks Java HTTP clients via TLS fingerprinting) ---

    private Map<String, Object> get(String path) {
        String url = baseUrl() + path;
        log.info("-> GET {}", url);

        try {
            List<String> command = new ArrayList<>();
            command.add("curl");
            command.add("-s");
            command.add("-X");
            command.add("GET");
            command.add(url);
            command.add("-H");
            command.add(AUTH_HEADER_PREFIX + properties.getApiKey());
            command.add("-H");
            command.add("Accept: application/json");
            command.add("-w");
            command.add(CURL_STATUS_SUFFIX);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor(30, TimeUnit.SECONDS);

            return parseResponse("GET", url, output);
        } catch (BlindPayApiException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BlindPayApiException(500, "Interrupted while calling GET " + url);
        } catch (Exception ex) {
            log.error("Error calling BlindPay API GET {}: {}", url, ex.getMessage(), ex);
            throw new BlindPayApiException(500, "Failed to call BlindPay API: " + url);
        }
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        String url = baseUrl() + path;

        try {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("-> POST {} | body: {}", url, requestJson);

            List<String> command = new ArrayList<>();
            command.add("curl");
            command.add("-s");
            command.add("-X");
            command.add("POST");
            command.add(url);
            command.add("-H");
            command.add(AUTH_HEADER_PREFIX + properties.getApiKey());
            command.add("-H");
            command.add("Content-Type: application/json");
            command.add("-H");
            command.add("Accept: application/json");
            command.add("-d");
            command.add(requestJson);
            command.add("-w");
            command.add(CURL_STATUS_SUFFIX);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor(30, TimeUnit.SECONDS);

            return parseResponse("POST", url, output);
        } catch (BlindPayApiException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BlindPayApiException(500, "Interrupted while calling POST " + url);
        } catch (Exception ex) {
            log.error("Error calling BlindPay API POST {}: {}", url, ex.getMessage(), ex);
            throw new BlindPayApiException(500, "Failed to call BlindPay API: " + url);
        }
    }

    private String uploadForm(String path, String filePath, String bucket) {
        String url = baseUrl() + path;
        log.info("-> UPLOAD {} | file: {}, bucket: {}", url, filePath, bucket);

        try {
            List<String> command = new ArrayList<>();
            command.add("curl");
            command.add("-s");
            command.add("-X");
            command.add("POST");
            command.add(url);
            command.add("-H");
            command.add(AUTH_HEADER_PREFIX + properties.getApiKey());
            command.add("-F");
            command.add("file=@" + filePath);
            command.add("-F");
            command.add("bucket=" + bucket);
            command.add("-w");
            command.add(CURL_STATUS_SUFFIX);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor(30, TimeUnit.SECONDS);

            Map<String, Object> result = parseResponse("UPLOAD", url, output);
            String fileUrl = (String) result.get("file_url");
            log.info("<- Uploaded file URL: {}", fileUrl);
            return fileUrl;
        } catch (BlindPayApiException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BlindPayApiException(500, "Interrupted while uploading to " + url);
        } catch (Exception ex) {
            log.error("Error uploading to BlindPay API {}: {}", url, ex.getMessage(), ex);
            throw new BlindPayApiException(500, "Failed to upload to BlindPay API: " + url);
        }
    }

    private Map<String, Object> parseResponse(String method, String url, String output)
            throws IOException {
        String[] lines = output.split("\n");
        String statusLine = lines[lines.length - 1].trim();
        String responseBody = output.substring(0, output.lastIndexOf("\n")).trim();

        int statusCode;
        try {
            statusCode = Integer.parseInt(statusLine);
        } catch (NumberFormatException e) {
            log.error("<- {} {} | failed to parse response: {}", method, url, output);
            throw new BlindPayApiException(500, "Failed to parse curl response for " + url);
        }

        log.info("<- {} {} | status: {} | response: {}", method, url, statusCode, responseBody);

        if (statusCode >= 400) {
            throw new BlindPayApiException(statusCode, responseBody);
        }

        return objectMapper.readValue(responseBody, new TypeReference<>() {});
    }
}
