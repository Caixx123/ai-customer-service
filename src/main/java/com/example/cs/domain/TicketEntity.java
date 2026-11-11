package com.example.cs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_no", nullable = false, unique = true, length = 64)
    private String ticketNo;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TicketEntity() {
    }

    public TicketEntity(String ticketNo, String userId, String sessionId,
                        String reason, String summary, String status, LocalDateTime createdAt) {
        this.ticketNo = ticketNo;
        this.userId = userId;
        this.sessionId = sessionId;
        this.reason = reason;
        this.summary = summary;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getReason() {
        return reason;
    }

    public String getSummary() {
        return summary;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
