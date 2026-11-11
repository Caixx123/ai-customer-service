package com.example.cs.domain;

public enum OrderStatus {
    PAID("待发货"),
    SHIPPED("运输中"),
    DELIVERED("已签收"),
    REFUNDING("退款中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
