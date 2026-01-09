package com.pizza.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {
    private String orderId;
    private String pizza;
    private Integer quantity;
    private String address;
    private String customerName;
    private LocalDateTime timestamp;
}
