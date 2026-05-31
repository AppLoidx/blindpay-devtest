package com.example.blindpay.dto;

import com.example.blindpay.model.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        String receiverId,
        String bankAccountId,
        String blockchainWalletId,
        String walletId,
        String walletAddress,
        String tosId
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getReceiverId(),
                user.getBankAccountId(),
                user.getBlockchainWalletId(),
                user.getWalletId(),
                user.getWalletAddress(),
                user.getTosId()
        );
    }
}
