package com.pizza.kitchen.service;

import com.pizza.kitchen.config.RabbitMQConfig;
import com.pizza.kitchen.model.OrderPlacedEvent;
import com.pizza.kitchen.model.OrderReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class KitchenService {

    private static final Logger logger = LoggerFactory.getLogger(KitchenService.class);
    private final Random random = new Random();
    private final RabbitTemplate rabbitTemplate;
    private final String instanceId;

    @Value("${kitchen.preparation.time.min:5000}")
    private int preparationTimeMin;

    @Value("${kitchen.preparation.time.max:10000}")
    private int preparationTimeMax;

    public KitchenService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        // Generate unique instance ID to demonstrate competing consumers
        String id;
        try {
            id = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            id = "kitchen-" + random.nextInt(1000);
        }
        this.instanceId = id;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_PLACED_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        logger.info("[{}] Received order {} - {} x {} for {}", 
            instanceId, event.getOrderId(), event.getQuantity(), event.getPizza(), event.getCustomerName());

        try {
            // Simulate pizza preparation time
            int preparationTime = preparationTimeMin + random.nextInt(preparationTimeMax - preparationTimeMin);
            logger.info("[{}] Preparing order {} - estimated time: {} ms", 
                instanceId, event.getOrderId(), preparationTime);
            
            Thread.sleep(preparationTime);
            
            logger.info("[{}] Order {} is ready!", instanceId, event.getOrderId());

            // Publish order ready event
            OrderReadyEvent readyEvent = new OrderReadyEvent(
                event.getOrderId(),
                event.getPizza(),
                event.getQuantity(),
                event.getAddress(),
                event.getCustomerName(),
                LocalDateTime.now()
            );

            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_READY_QUEUE, readyEvent);
            logger.info("[{}] Published order.ready event for order {}", instanceId, event.getOrderId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[{}] Order preparation interrupted for order {}", instanceId, event.getOrderId(), e);
        } catch (Exception e) {
            logger.error("[{}] Error processing order {}: {}", instanceId, event.getOrderId(), e.getMessage(), e);
        }
    }
}
