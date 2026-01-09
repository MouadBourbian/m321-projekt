package com.pizza.delivery.service;

import com.pizza.delivery.config.RabbitMQConfig;
import com.pizza.delivery.model.DeliveryStatus;
import com.pizza.delivery.model.OrderReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);
    private final Random random = new Random();
    private final Map<String, DeliveryStatus> deliveries = new ConcurrentHashMap<>();

    private static final String[] DRIVER_NAMES = {
        "Max Mustermann", "Anna Schmidt", "Peter Mueller", "Lisa Weber", "Tom Fischer"
    };

    @RabbitListener(queues = RabbitMQConfig.ORDER_READY_QUEUE)
    public void handleOrderReady(OrderReadyEvent event) {
        logger.info("Received order.ready event for order {}", event.getOrderId());

        // Simulate driver assignment
        String driverName = DRIVER_NAMES[random.nextInt(DRIVER_NAMES.length)];
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime estimatedDelivery = now.plusMinutes(20 + random.nextInt(20)); // 20-40 minutes

        DeliveryStatus status = new DeliveryStatus(
            event.getOrderId(),
            "ASSIGNED",
            driverName,
            event.getAddress(),
            now,
            estimatedDelivery
        );

        deliveries.put(event.getOrderId(), status);

        logger.info("Order {} assigned to driver {} for delivery to {}", 
            event.getOrderId(), driverName, event.getAddress());

        // Simulate customer notification
        sendNotification(event, driverName, estimatedDelivery);
    }

    private void sendNotification(OrderReadyEvent event, String driverName, LocalDateTime estimatedDelivery) {
        logger.info("=== CUSTOMER NOTIFICATION ===");
        logger.info("Dear {}, your order is on its way!", event.getCustomerName());
        logger.info("Order ID: {}", event.getOrderId());
        logger.info("Items: {} x {}", event.getQuantity(), event.getPizza());
        logger.info("Driver: {}", driverName);
        logger.info("Delivery Address: {}", event.getAddress());
        logger.info("Estimated Delivery Time: {}", estimatedDelivery);
        logger.info("=============================");
    }

    public DeliveryStatus getDeliveryStatus(String orderId) {
        return deliveries.get(orderId);
    }

    public Map<String, DeliveryStatus> getAllDeliveries() {
        return deliveries;
    }
}
