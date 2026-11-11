package com.example.cs.domain.repository;

import com.example.cs.domain.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {
}
