package com.example.cs.domain.repository;

import com.example.cs.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderNoAndUserId(String orderNo, String userId);

    List<OrderEntity> findTop5ByUserIdOrderByCreatedAtDesc(String userId);
}
