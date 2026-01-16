package com.pizza.delivery.service;

import com.pizza.delivery.config.RabbitMQConfig;
import com.pizza.delivery.model.DeliveryStatus;
import com.pizza.delivery.model.OrderReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
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
    
    // Time in seconds after assignment when order goes IN_TRANSIT (10-15 seconds)
    private static final int MIN_IN_TRANSIT_TIME = 10;
    private static final int MAX_IN_TRANSIT_TIME = 15;
    
    // Time in seconds after IN_TRANSIT when order is DELIVERED (15-25 seconds)
    private static final int MIN_DELIVERY_TIME = 15;
    private static final int MAX_DELIVERY_TIME = 25;

    @RabbitListener(queues = RabbitMQConfig.ORDER_READY_QUEUE)
    public void handleOrderReady(OrderReadyEvent event) {
        logger.info("Received order.ready event for order {}", event.getOrderId());

        // Simulate driver assignment
        String driverName = DRIVER_NAMES[random.nextInt(DRIVER_NAMES.length)];
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime estimatedDelivery = now.plusMinutes(20 + random.nextInt(20)); // 20-40 minutes
        
        // Calculate target transition times upfront
        int inTransitSeconds = MIN_IN_TRANSIT_TIME + random.nextInt(MAX_IN_TRANSIT_TIME - MIN_IN_TRANSIT_TIME + 1);
        LocalDateTime targetInTransitTime = now.plusSeconds(inTransitSeconds);

        DeliveryStatus status = new DeliveryStatus(
            event.getOrderId(),
            "ASSIGNED",
            driverName,
            event.getAddress(),
            now,
            estimatedDelivery,
            null,
            null,
            targetInTransitTime,
            null
        );

        deliveries.put(event.getOrderId(), status);

        logger.info("Order {} assigned to driver {} for delivery to {}", 
            event.getOrderId(), driverName, event.getAddress());

        // Simulate customer notification
        sendNotification(event, driverName, estimatedDelivery);
    }
    
    /**
     * Scheduled task that runs every 5 seconds to update delivery statuses
     * - ASSIGNED -> IN_TRANSIT after 10-15 seconds (target time calculated on assignment)
     * - IN_TRANSIT -> DELIVERED after 15-25 seconds (target time calculated on transition to IN_TRANSIT)
     */
    @Scheduled(fixedRate = 5000)
    public void updateDeliveryStatuses() {
        LocalDateTime now = LocalDateTime.now();
        
        deliveries.values().forEach(delivery -> {
            if ("ASSIGNED".equals(delivery.getStatus())) {
                // Check if target time for IN_TRANSIT has been reached
                if (now.isAfter(delivery.getTargetInTransitTime()) || now.isEqual(delivery.getTargetInTransitTime())) {
                    delivery.setStatus("IN_TRANSIT");
                    delivery.setInTransitAt(now);
                    
                    // Calculate target time for delivery
                    int deliverySeconds = MIN_DELIVERY_TIME + random.nextInt(MAX_DELIVERY_TIME - MIN_DELIVERY_TIME + 1);
                    delivery.setTargetDeliveredTime(now.plusSeconds(deliverySeconds));
                    
                    logger.info("Order {} status changed to IN_TRANSIT (driver {} on the way)", 
                        delivery.getOrderId(), delivery.getDriverName());
                }
            } else if ("IN_TRANSIT".equals(delivery.getStatus()) && delivery.getTargetDeliveredTime() != null) {
                // Check if target time for DELIVERED has been reached
                if (now.isAfter(delivery.getTargetDeliveredTime()) || now.isEqual(delivery.getTargetDeliveredTime())) {
                    delivery.setStatus("DELIVERED");
                    delivery.setDeliveredAt(now);
                    logger.info("Order {} has been DELIVERED to {} by {}", 
                        delivery.getOrderId(), delivery.getAddress(), delivery.getDriverName());
                }
            }
        });
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
