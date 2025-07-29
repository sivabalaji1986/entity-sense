package com.hbs.entitysense.service;

import com.hbs.entitysense.dto.*;
import com.hbs.entitysense.entity.WatchlistEntity;
import com.hbs.entitysense.model.RiskCategory;
import com.hbs.entitysense.repository.WatchlistRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EntitySenseServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private EntitySenseService entitySenseService;

    public EntitySenseServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateSanctionEntity() {
        CreateWatchListEntityRequest request = new CreateWatchListEntityRequest();
        request.setName("Test Corp");
        request.setCountry("SG");
        request.setAddress("10 Street");
        request.setRiskCategory(RiskCategory.SANCTION);

        WatchlistEntity savedEntity = new WatchlistEntity();
        savedEntity.setName("Test Corp");

        when(watchlistRepository.save(any())).thenReturn(savedEntity);

        assertDoesNotThrow(() -> entitySenseService.createWatchListEntity(request));
    }

    @Test
    public void testValidatePaymentReturnsAllowWhenNoMatch() {
        ValidatePaymentRequest request = new ValidatePaymentRequest();
        request.setPayeeName("Unknown Corp");
        request.setPayeeCountry("US");
        request.setPayeeAddress("123 Lane");

        // Simulate no matches from DB
        when(watchlistRepository.findAll()).thenReturn(Collections.emptyList());

        ValidatePaymentResponse response = entitySenseService.validatePayment(request);
        assertEquals("ALLOW", response.getStatus());
        assertTrue(response.getPossibleWatchListEntityMatches().isEmpty());
    }
}
