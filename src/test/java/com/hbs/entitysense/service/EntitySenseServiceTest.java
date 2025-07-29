package com.hbs.entitysense.service;

import com.hbs.entitysense.dto.CreateWatchListEntityRequest;
import com.hbs.entitysense.dto.OllamaEmbeddingResponse;
import com.hbs.entitysense.dto.ValidatePaymentRequest;
import com.hbs.entitysense.dto.ValidatePaymentResponse;
import com.hbs.entitysense.entity.WatchlistEntity;
import com.hbs.entitysense.model.RiskCategory;
import com.hbs.entitysense.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EntitySenseServiceTest {

    private WebClient webClient;
    private WatchlistRepository watchlistRepository;
    private EntitySenseService entitySenseService;

    @BeforeEach
    public void setUp() {
        webClient = mock(WebClient.class);
        watchlistRepository = mock(WatchlistRepository.class);
        entitySenseService = new EntitySenseService(webClient, watchlistRepository);
    }

    @Test
    public void testValidatePaymentReturnsAllowWhenNoMatch() {
        ValidatePaymentRequest request = new ValidatePaymentRequest("John Doe", "123 Main St", "USA", "ACCT001");

        // Mock WebClient behavior
        RequestBodyUriSpec requestBodyUriSpec = mock(RequestBodyUriSpec.class);
        RequestHeadersSpec<?> requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        // Create mock embedding response
        OllamaEmbeddingResponse mockResponse = new OllamaEmbeddingResponse();
        float[] embeddingArray = new float[768];
        for (int i = 0; i < 768; i++) {
            embeddingArray[i] = 0.01f;
        }
        List<Float> embeddingList = new ArrayList<>();
        for (float f : embeddingArray) {
            embeddingList.add(f);
        }
        mockResponse.setEmbedding(embeddingList);

        // Stub WebClient chain
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(eq(HttpHeaders.CONTENT_TYPE), eq(MediaType.APPLICATION_JSON_VALUE))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenAnswer(inv -> requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OllamaEmbeddingResponse.class)).thenReturn(Mono.just(mockResponse));

        when(watchlistRepository.findAll()).thenReturn(Collections.emptyList());

        ValidatePaymentResponse response = entitySenseService.validatePayment(request);

        assertNotNull(response);
        assertEquals("ALLOW", response.getStatus());
        assertTrue(response.getPossibleWatchListEntityMatches().isEmpty());
    }

    @Test
    public void testCreateWatchListEntity() {
        CreateWatchListEntityRequest request = new CreateWatchListEntityRequest();
        request.setName("Bad Actor");
        request.setAddress("No. 404 Scam St");
        request.setCountry("RU");
        request.setRiskCategory(RiskCategory.SCAM_ENTITY);
        request.setKnownAccounts(List.of("AC12345"));

        // Mock WebClient behavior
        RequestBodyUriSpec requestBodyUriSpec = mock(RequestBodyUriSpec.class);
        RequestHeadersSpec<?> requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        OllamaEmbeddingResponse mockResponse = new OllamaEmbeddingResponse();
        float[] embeddingArray = new float[768];
        for (int i = 0; i < 768; i++) {
            embeddingArray[i] = 0.02f;
        }
        List<Float> embeddingList = new ArrayList<>();
        for (float f : embeddingArray) {
            embeddingList.add(f);
        }
        mockResponse.setEmbedding(embeddingList);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(eq(HttpHeaders.CONTENT_TYPE), eq(MediaType.APPLICATION_JSON_VALUE))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenAnswer(inv -> requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OllamaEmbeddingResponse.class)).thenReturn(Mono.just(mockResponse));

        when(watchlistRepository.save(any())).thenReturn(new WatchlistEntity());

        assertDoesNotThrow(() -> entitySenseService.createWatchListEntity(request));
    }
}