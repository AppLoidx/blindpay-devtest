package com.example.blindpay.service;

import com.example.blindpay.model.User;

import java.util.List;
import java.util.Map;

public interface UserServiceApi {

    long getUserCount();

    List<User> getAllUsers();

    User getUser(Long id);

    Map<String, Object> payin(Long userId, int amount);

    Map<String, Object> payout(Long userId, int amount);

    Map<String, Object> getBalance(Long userId);

    Map<String, Object> transfer(Long fromUserId, Long toUserId, int amount);
}
