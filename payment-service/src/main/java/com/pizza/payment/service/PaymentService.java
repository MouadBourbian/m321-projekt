package com.pizza.payment.service;

import com.pizza.payment.model.PaymentRequest;
import com.pizza.payment.model.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final Random random = new Random();

    @Value("${payment.failure.rate:0.2}")
    private double failureRate;

    @Value("${payment.delay.min:100}")
    private int delayMin;

    @Value("${payment.delay.max:500}")
    private int delayMax;

    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        logger.info("Processing payment for order {} with amount {}", 
            paymentRequest.getOrderId(), paymentRequest.getAmount());

        // Simulate processing delay
        try {
            int delay = delayMin + random.nextInt(delayMax - delayMin);
            Thread.sleep(delay);
            logger.debug("Payment processing delayed by {} ms", delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Payment processing interrupted", e);
        }

        // Simulate random failures to test resilience
        boolean shouldFail = random.nextDouble() < failureRate;
        
        if (shouldFail) {
            logger.warn("Payment failed for order {} (simulated failure)", 
                paymentRequest.getOrderId());
            return new PaymentResponse(
                null,
                false,
                "Payment declined by bank. Please try a different payment method."
            );
        }

        String transactionId = UUID.randomUUID().toString();
        logger.info("Payment successful for order {}, transaction ID: {}", 
            paymentRequest.getOrderId(), transactionId);

        return new PaymentResponse(
            transactionId,
            true,
            "Payment processed successfully"
        );
    }
}
