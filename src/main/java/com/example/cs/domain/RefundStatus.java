package com.example.cs.domain;

public enum RefundStatus {
    APPLIED("已申请"),
    MERCHANT_REVIEW("商家审核中"),
    APPROVED("已通过"),
    REFUNDED("已退款"),
    REJECTED("已拒绝");

    private final String label;

    RefundStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
