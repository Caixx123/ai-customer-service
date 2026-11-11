package com.example.cs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
public class RefundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RefundStatus status;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RefundEntity() {
    }

    public RefundEntity(String orderNo, String userId, BigDecimal amount, RefundStatus status,
                        String reason, LocalDateTime appliedAt, LocalDateTime updatedAt) {
        this.orderNo = orderNo;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.appliedAt = appliedAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
