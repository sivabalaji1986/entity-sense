package com.hbs.entitysense.controller;

import com.hbs.entitysense.dto.*;
import com.hbs.entitysense.service.EntitySenseService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EntitySenseControllerTest {

    @Mock
    private EntitySenseService entitySenseService;

    @InjectMocks
    private EntitySenseController controller;

    public EntitySenseControllerTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testValidatePaymentEndpoint() {
        ValidatePaymentRequest request = new ValidatePaymentRequest();
        request.setPayeeName("Test");
        request.setPayeeCountry("IN");

        ValidatePaymentResponse mockResponse = new ValidatePaymentResponse();
        mockResponse.setStatus("ALLOW");
        mockResponse.setPossibleWatchListEntityMatches(Collections.emptyList());

        when(entitySenseService.validatePayment(any())).thenReturn(mockResponse);

        ResponseEntity<ValidatePaymentResponse> response = controller.validatePayment(request);
        assertEquals("ALLOW", response.getBody().getStatus());
    }

    @Test
    public void testCreateWatchListEntityEndpoint() {
        CreateWatchListEntityRequest request = new CreateWatchListEntityRequest();
        request.setName("WatchCorp");
        request.setCountry("SG");

        doNothing().when(entitySenseService).createWatchListEntity(any());

        ResponseEntity<String> response = controller.createWatchListEntity(request);
        assertEquals("Entity created successfully", response.getBody());
    }
}
