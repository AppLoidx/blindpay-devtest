package com.example.blindpay.service;

import java.util.Map;

public interface BlindPayApi {

    Map<String, Object> createTosUrl(String redirectUrl);

    String uploadFile(String filePath);

    Map<String, Object> createReceiver(String firstName, String lastName,
                                       String email, String tosId,
                                       String selfieFileUrl, String idDocFrontFileUrl);

    Map<String, Object> getReceiver(String receiverId);

    Map<String, Object> createBankAccount(String receiverId,
                                          String name, String pixKey);

    Map<String, Object> createBlockchainWallet(String receiverId,
                                               String name,
                                               String walletAddress);

    Map<String, Object> createWallet(String receiverId, String name);

    Map<String, Object> getWallet(String receiverId, String walletId);

    Map<String, Object> getWalletBalance(String receiverId, String walletId);

    Map<String, Object> createPayinQuote(String walletId, int amount);

    Map<String, Object> createPayin(String payinQuoteId);

    Map<String, Object> createPayoutQuote(String bankAccountId, int amount);

    Map<String, Object> createPayout(String quoteId, String senderWalletAddress);

    Map<String, Object> createTransferQuote(String walletId,
                                            String receiverWalletAddress,
                                            int amount);

    Map<String, Object> createTransfer(String transferQuoteId);
}
