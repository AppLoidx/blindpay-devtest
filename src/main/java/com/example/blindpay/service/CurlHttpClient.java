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

    private static final String AUTH_HEADER_PREFIX = "Authorization: Bearer ";
    private static final String CURL_STATUS_SUFFIX = "\n%{http_code}";

    private final BlindPayProperties properties;
    private final ObjectMapper objectMapper;

    public Map<String, Object> get(String url) {
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

            return execute(command, "GET", url);
        } catch (BlindPayApiException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BlindPayApiException(500, "Interrupted while calling GET " + url);
        } catch (Exception ex) {
            log.error("Error calling GET {}: {}", url, ex.getMessage(), ex);
            throw new BlindPayApiException(500, "Failed to call GET: " + url);
        }
    }

    public Map<String, Object> post(String url, Map<String, Object> body) {
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

            return execute(command, "POST", url);
        } catch (BlindPayApiException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BlindPayApiException(500, "Interrupted while calling POST " + url);
        } catch (Exception ex) {
            log.error("Error calling POST {}: {}", url, ex.getMessage(), ex);
            throw new BlindPayApiException(500, "Failed to call POST: " + url);
        }
    }

    public String uploadForm(String url, String filePath, String bucket) {
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

            Map<String, Object> result = execute(command, "UPLOAD", url);
            String fileUrl = (String) result.get("file_url");
            log.info("<- Uploaded file URL: {}", fileUrl);
            return fileUrl;
        } catch (BlindPayApiException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BlindPayApiException(500, "Interrupted while uploading to " + url);
        } catch (Exception ex) {
            log.error("Error uploading to {}: {}", url, ex.getMessage(), ex);
            throw new BlindPayApiException(500, "Failed to upload to: " + url);
        }
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
