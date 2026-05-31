package com.example.blindpay.dto;

import lombok.Data;

@Data
public class PayoutRequest {
    private int amount; // cents: 10000 = $100.00
}
