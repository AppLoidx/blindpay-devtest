package com.example.blindpay.repository;

import com.example.blindpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
