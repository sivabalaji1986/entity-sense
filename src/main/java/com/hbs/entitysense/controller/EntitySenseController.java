package com.hbs.entitysense.controller;

import com.hbs.entitysense.dto.*;
import com.hbs.entitysense.service.EntitySenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Entity Validation", description = "Endpoints to check for risky entities during payment processing")
public class EntitySenseController {

    private final EntitySenseService entitySenseService;

    private static final Logger logger = LoggerFactory.getLogger(EntitySenseController.class);

    @PostMapping("/create-watch-list-entity")
    @Operation(summary = "Create Watch List Entity", description = "Add a new sanctioned or high-risk entity to the watchlist")
    public ResponseEntity<String> createWatchListEntity(@Valid @RequestBody CreateWatchListEntityRequest request) {
        logger.info("Received request to create watch list entity: {}", request);
        entitySenseService.createWatchListEntity(request);
        return ResponseEntity.ok("Entity created successfully");
    }

    @PostMapping("/validate-payment")
    @Operation(summary = "Validate Payment", description = "Checks if a payee is a potential match to known sanctioned or mule entities")
    public ResponseEntity<ValidatePaymentResponse> validatePayment(@Valid @RequestBody ValidatePaymentRequest request) {
        logger.info("Received request to validate payment: {}", request);
        return ResponseEntity.ok(entitySenseService.validatePayment(request));
    }
}
