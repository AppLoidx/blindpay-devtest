package com.example.blindpay.service;

import com.example.blindpay.config.BlindPayProperties;
import com.example.blindpay.exception.BlindPayApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurlHttpClient {

    private static final String CURL_CMD = "curl";
    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";
    private static final String AUTH_HEADER_PREFIX = "Authorization: Bearer ";
    private static final String CURL_STATUS_SUFFIX = "\n%{http_code}";

    private final BlindPayProperties properties;
    private final ObjectMapper objectMapper;

    public Map<String, Object> get(String url) {
        log.info("-> GET {}", url);
        return withErrorHandling(GET_METHOD, url, () -> {
            List<String> cmd = baseCommand(GET_METHOD, url);
            addJsonAcceptHeader(cmd);
            addStatusSuffix(cmd);
            return execute(cmd, GET_METHOD, url);
        });
    }

    public Map<String, Object> post(String url, Map<String, Object> body) {
        return withErrorHandling(POST_METHOD, url, () -> {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("-> POST {} | body: {}", url, requestJson);
            List<String> cmd = baseCommand(POST_METHOD, url);
            addHeader(cmd, "Content-Type: application/json");
            addJsonAcceptHeader(cmd);
            addPayload(cmd, requestJson);
            addStatusSuffix(cmd);
            return execute(cmd, POST_METHOD, url);
        });
    }

    public String uploadForm(String url, String filePath, String bucket) {
        log.info("-> UPLOAD {} | file: {}, bucket: {}", url, filePath, bucket);
        Map<String, Object> result = withErrorHandling("UPLOAD", url, () -> {
            List<String> cmd = baseCommand(POST_METHOD, url);
            addFormField(cmd, "file=@" + filePath);
            addFormField(cmd, "bucket=" + bucket);
            addStatusSuffix(cmd);
            return execute(cmd, "UPLOAD", url);
        });
        String fileUrl = (String) result.get("file_url");
        log.info("<- Uploaded file URL: {}", fileUrl);
        return fileUrl;
    }

    private <T> T withErrorHandling(String method, String url, CurlOperation<T> operation) {
        try {
            return operation.execute();
        } catch (BlindPayApiException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BlindPayApiException(500, "Interrupted while calling " + method + " " + url);
        } catch (Exception ex) {
            log.error("Error calling {} {}: {}", method, url, ex.getMessage(), ex);
            throw new BlindPayApiException(500, "Failed to call " + method + ": " + url);
        }
    }

    @FunctionalInterface
    private interface CurlOperation<T> {
        T execute() throws Exception;
    }

    private List<String> baseCommand(String method, String url) {
        List<String> cmd = new ArrayList<>();
        cmd.add(CURL_CMD);
        cmd.add("-s");
        cmd.add("-X");
        cmd.add(method);
        cmd.add(url);
        addHeader(cmd, AUTH_HEADER_PREFIX + properties.getApiKey());
        return cmd;
    }

    private void addHeader(List<String> cmd, String header) {
        cmd.add("-H");
        cmd.add(header);
    }

    private void addJsonAcceptHeader(List<String> cmd) {
        addHeader(cmd, "Accept: application/json");
    }

    private void addPayload(List<String> cmd, String json) {
        cmd.add("-d");
        cmd.add(json);
    }

    private void addFormField(List<String> cmd, String field) {
        cmd.add("-F");
        cmd.add(field);
    }

    private void addStatusSuffix(List<String> cmd) {
        cmd.add("-w");
        cmd.add(CURL_STATUS_SUFFIX);
    }

    private Map<String, Object> execute(List<String> command, String method, String url)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor(30, TimeUnit.SECONDS);
        return parseResponse(method, url, output);
    }

    private Map<String, Object> parseResponse(String method, String url, String output)
            throws IOException {
        String[] lines = output.split("\n");
        String statusLine = lines[lines.length - 1].trim();
        String responseBody = output.substring(0, output.lastIndexOf("\n")).trim();
        int statusCode = parseStatusCode(method, url, statusLine, output);
        log.info("<- {} {} | status: {} | response: {}", method, url, statusCode, responseBody);
        if (statusCode >= 400) {
            throw new BlindPayApiException(statusCode, responseBody);
        }
        return objectMapper.readValue(responseBody, new TypeReference<>() {});
    }

    private int parseStatusCode(String method, String url, String statusLine, String output) {
        try {
            return Integer.parseInt(statusLine);
        } catch (NumberFormatException e) {
            log.error("<- {} {} | failed to parse response: {}", method, url, output);
            throw new BlindPayApiException(500, "Failed to parse curl response for " + url);
        }
    }
}
