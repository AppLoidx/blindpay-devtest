package com.example.blindpay.dto;

import lombok.Data;

@Data
public class TransferRequest {
    private Long fromUserId;
    private Long toUserId;
    private int amount; // cents: 5000 = $50.00
}
