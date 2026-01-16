package com.pizza.delivery.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatus {
    private String orderId;
    private String status;
    private String driverName;
    private String address;
    private LocalDateTime assignedAt;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime deliveredAt;
    private LocalDateTime inTransitAt;
}
