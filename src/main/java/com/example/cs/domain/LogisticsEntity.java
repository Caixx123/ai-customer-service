package com.example.cs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "logistics")
public class LogisticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(nullable = false, length = 64)
    private String carrier;

    @Column(name = "tracking_no", nullable = false, length = 64)
    private String trackingNo;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(nullable = false, length = 1000)
    private String detail;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LogisticsEntity() {
    }

    public LogisticsEntity(String orderNo, String carrier, String trackingNo,
                           String status, String detail, LocalDateTime updatedAt) {
        this.orderNo = orderNo;
        this.carrier = carrier;
        this.trackingNo = trackingNo;
        this.status = status;
        this.detail = detail;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public String getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
