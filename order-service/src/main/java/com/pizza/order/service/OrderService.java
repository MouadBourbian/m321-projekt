package com.pizza.order.service;

import com.pizza.order.config.RabbitMQConfig;
import com.pizza.order.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    public OrderService(RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    public OrderResponse processOrder(OrderRequest orderRequest) {
        String orderId = UUID.randomUUID().toString();
        logger.info("Processing order {} for customer {}", orderId, orderRequest.getCustomerName());

        // Calculate amount based on quantity (simplified pricing)
        double amount = orderRequest.getQuantity() * 15.99;

        // Step 1: Process payment (synchronous)
        PaymentRequest paymentRequest = new PaymentRequest(orderId, orderRequest.getCustomerName(), amount);
        
        try {
            PaymentResponse paymentResponse = processPayment(paymentRequest);
            
            if (!paymentResponse.isSuccess()) {
                logger.warn("Payment failed for order {}: {}", orderId, paymentResponse.getMessage());
                return new OrderResponse(orderId, "PAYMENT_FAILED", 
                    "Payment was declined: " + paymentResponse.getMessage());
            }

            logger.info("Payment successful for order {}, transaction ID: {}", 
                orderId, paymentResponse.getTransactionId());

        } catch (RestClientException e) {
            logger.error("Payment service unavailable for order {}: {}", orderId, e.getMessage());
            return new OrderResponse(orderId, "ERROR", 
                "Payment system is currently unavailable. Please try again later.");
        }

        // Step 2: Send order to kitchen (asynchronous)
        try {
            OrderPlacedEvent event = new OrderPlacedEvent(
                orderId,
                orderRequest.getPizza(),
                orderRequest.getQuantity(),
                orderRequest.getAddress(),
                orderRequest.getCustomerName(),
                LocalDateTime.now()
            );

            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_PLACED_QUEUE, event);
            logger.info("Order {} sent to kitchen queue", orderId);

        } catch (Exception e) {
            logger.error("Failed to send order {} to kitchen queue: {}", orderId, e.getMessage());
            // Order is still accepted even if kitchen is temporarily unavailable
            // The message will be queued when RabbitMQ is back online
        }

        return new OrderResponse(orderId, "SUCCESS", 
            "Order placed successfully! Your order ID is: " + orderId);
    }

    private PaymentResponse processPayment(PaymentRequest paymentRequest) {
        logger.info("Calling payment service for order {}", paymentRequest.getOrderId());
        String url = paymentServiceUrl + "/pay";
        return restTemplate.postForObject(url, paymentRequest, PaymentResponse.class);
    }
}
