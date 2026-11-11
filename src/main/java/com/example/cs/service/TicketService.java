package com.example.cs.service;

import com.example.cs.domain.TicketEntity;
import com.example.cs.domain.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TicketService {

    private static final DateTimeFormatter TICKET_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public String create(String userId, String sessionId, String reason, String summary) {
        String ticketNo = "T" + LocalDateTime.now().format(TICKET_TIME)
                + ThreadLocalRandom.current().nextInt(100, 1000);
        TicketEntity ticket = new TicketEntity(
                ticketNo,
                userId,
                sessionId,
                truncate(reason, 255),
                truncate(summary, 1000),
                "OPEN",
                LocalDateTime.now());
        ticketRepository.save(ticket);
        return ticketNo;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "未提供";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
