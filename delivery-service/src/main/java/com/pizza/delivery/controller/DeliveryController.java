package com.pizza.delivery.controller;

import com.pizza.delivery.model.DeliveryStatus;
import com.pizza.delivery.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<DeliveryStatus> getDeliveryStatus(@PathVariable String orderId) {
        logger.info("Checking delivery status for order {}", orderId);
        
        DeliveryStatus status = deliveryService.getDeliveryStatus(orderId);
        
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(status);
    }

    @GetMapping
    public ResponseEntity<Map<String, DeliveryStatus>> getAllDeliveries() {
        logger.info("Fetching all deliveries");
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Delivery Service is running");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body("An unexpected error occurred.");
    }
}
