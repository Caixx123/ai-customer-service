package com.example.cs.domain.repository;

import com.example.cs.domain.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<RefundEntity, Long> {

    Optional<RefundEntity> findByOrderNoAndUserId(String orderNo, String userId);
}
