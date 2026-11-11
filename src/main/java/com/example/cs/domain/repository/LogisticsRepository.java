package com.example.cs.domain.repository;

import com.example.cs.domain.LogisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LogisticsRepository extends JpaRepository<LogisticsEntity, Long> {

    Optional<LogisticsEntity> findByOrderNo(String orderNo);
}
