package com.pizza.kitchen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderReadyEvent {
    private String orderId;
    private String pizza;
    private Integer quantity;
    private String address;
    private String customerName;
    private LocalDateTime preparedAt;
}
