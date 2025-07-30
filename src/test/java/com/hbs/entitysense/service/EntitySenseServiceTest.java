package com.hbs.entitysense.service;

import com.hbs.entitysense.dto.WatchListEntityRequest;
import com.hbs.entitysense.dto.OllamaEmbeddingResponse;
import com.hbs.entitysense.dto.ValidatePaymentRequest;
import com.hbs.entitysense.dto.ValidatePaymentResponse;
import com.hbs.entitysense.entity.WatchlistEntity;
import com.hbs.entitysense.model.RiskCategory;
import com.hbs.entitysense.repository.WatchlistRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EntitySenseServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EntitySenseService entitySenseService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Stub a successful Ollama response with a list of float values.
     */
    private void stubOllama(int size, float value) throws Exception {
        // Prepare DTO
        OllamaEmbeddingResponse dto = new OllamaEmbeddingResponse();
        List<Float> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(value);
        }
        dto.setEmbedding(list);

        // Prepare JSON strings
        String requestJson = "{\"model\":\"dummy\",\"prompt\":\"text\"}";
        String responseJson = "{\"embedding\":[]}";

        // Stub ObjectMapper for request and response
        when(objectMapper.writeValueAsString(any())).thenReturn(requestJson);
        when(objectMapper.readValue(eq(responseJson), eq(OllamaEmbeddingResponse.class))).thenReturn(dto);

        // Mock HTTP response
        @SuppressWarnings("unchecked")
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.body()).thenReturn(responseJson);

        // Stub HttpClient.send
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
    }

    /**
     * Stub an Ollama response with a null embedding list.
     */
    private void stubOllamaNull() throws Exception {
        // Prepare JSON strings
        String requestJson = "{\"model\":\"dummy\",\"prompt\":\"text\"}";
        String responseJson = "{\"embedding\":null}";

        // Stub ObjectMapper for request
        when(objectMapper.writeValueAsString(any())).thenReturn(requestJson);
        // Stub ObjectMapper for null response
        when(objectMapper.readValue(eq(responseJson), eq(OllamaEmbeddingResponse.class)))
                .thenReturn(new OllamaEmbeddingResponse());

        // Mock HTTP response with null body
        @SuppressWarnings("unchecked")
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.body()).thenReturn(responseJson);

        // Stub HttpClient.send
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
    }

    @Test
    public void testValidatePaymentReturnsAllowWhenNoMatch() throws Exception {
        stubOllama(768, 0.0f);
        when(watchlistRepository.findAll()).thenReturn(Collections.emptyList());

        ValidatePaymentRequest req = new ValidatePaymentRequest();
        req.setPayeeName("X");
        req.setPayeeAddress("A");
        req.setPayeeCountry("US");
        req.setAccountNumber("ACC0");

        ValidatePaymentResponse resp = entitySenseService.validatePayment(req);
        assertEquals("ALLOW", resp.getStatus());
        // Fixed: Changed from getPossibleWatchListEntityMatches to getPossibleWatchListEntityMatches
        assertTrue(resp.getPossibleWatchListEntityMatches().isEmpty());
    }

    @Test
    public void testValidatePaymentReturnsBlockOnVectorMatch() throws Exception {
        stubOllama(768, 1.0f);
        WatchlistEntity e = new WatchlistEntity();
        e.setId(1L);
        e.setName("A");
        e.setAddress("A");
        e.setCountry("US");
        e.setKnownAccounts(null);
        e.setRiskCategory(RiskCategory.SANCTION);

        float[] arr = new float[768];
        for (int i = 0; i < 768; i++) arr[i] = 1.0f;
        e.setEmbedding(arr);

        when(watchlistRepository.findAll()).thenReturn(List.of(e));

        ValidatePaymentRequest req = new ValidatePaymentRequest();
        req.setPayeeName("A");
        req.setPayeeAddress("A");
        req.setPayeeCountry("US");
        req.setAccountNumber("NONE");

        ValidatePaymentResponse resp = entitySenseService.validatePayment(req);
        assertEquals("BLOCK", resp.getStatus());
        assertEquals(1, resp.getPossibleWatchListEntityMatches().size());
    }

    @Test
    public void testCreateWatchListEntitySuccess() throws Exception {
        stubOllama(768, 0.42f);
        ArgumentCaptor<WatchlistEntity> captor = ArgumentCaptor.forClass(WatchlistEntity.class);
        when(watchlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WatchListEntityRequest req = new WatchListEntityRequest();
        req.setName("B");
        req.setAddress("B");
        req.setCountry("US");
        req.setRiskCategory(RiskCategory.CYBER_THREAT);
        req.setKnownAccounts(new String[]{"AC1"});

        assertDoesNotThrow(() -> entitySenseService.createWatchListEntity(req));
        verify(watchlistRepository).save(captor.capture());
        WatchlistEntity saved = captor.getValue();
        assertEquals("B", saved.getName());
        assertNotNull(saved.getEmbedding());
        assertEquals(768, saved.getEmbedding().length);
    }

    @Test
    public void testCreateWatchListEntityThrowsOnNullEmbedding() throws Exception {
        // Stub Ollama to return null embedding list
        stubOllamaNull();

        WatchListEntityRequest req = new WatchListEntityRequest();
        req.setName("NullTest");
        req.setAddress("X");
        req.setCountry("Y");
        req.setRiskCategory(RiskCategory.PEP);
        req.setKnownAccounts(new String[]{"AC1"});

        // since the service throws RuntimeException when embedding generation fails
        assertThrows(RuntimeException.class, () -> entitySenseService.createWatchListEntity(req));
    }

    @Test
    public void testCosineDistanceIdentical() throws Exception {
        java.lang.reflect.Method m = EntitySenseService.class.getDeclaredMethod(
                "cosineDistance", float[].class, float[].class);
        m.setAccessible(true);
        double d = (double) m.invoke(entitySenseService, new float[]{1f,0f}, new float[]{1f,0f});
        assertEquals(0.0, d, 1e-6);
    }

    // Additional test for edge cases
    @Test
    public void testValidatePaymentWithAccountMatch() throws Exception {
        stubOllama(768, 0.5f); // Distance above threshold but account matches
        WatchlistEntity e = new WatchlistEntity();
        e.setId(1L);
        e.setName("TestEntity");
        e.setAddress("TestAddress");
        e.setCountry("US");
        e.setKnownAccounts(new String[]{"ACC123","ACC456"});
        e.setRiskCategory(RiskCategory.MULE);

        float[] arr = new float[768];
        for (int i = 0; i < 768; i++) arr[i] = 0.1f; // Different from input to create distance
        e.setEmbedding(arr);

        when(watchlistRepository.findAll()).thenReturn(List.of(e));

        ValidatePaymentRequest req = new ValidatePaymentRequest();
        req.setPayeeName("DifferentName");
        req.setPayeeAddress("DifferentAddress");
        req.setPayeeCountry("CA");
        req.setAccountNumber("ACC123"); // This should match

        ValidatePaymentResponse resp = entitySenseService.validatePayment(req);
        assertEquals("BLOCK", resp.getStatus());
        assertEquals(1, resp.getPossibleWatchListEntityMatches().size());
        assertTrue(resp.getPossibleWatchListEntityMatches().get(0).isMatchedAccount());
    }

    @Test
    public void testCreateWatchListEntityWithNullFields() throws Exception {
        stubOllama(768, 0.1f);
        ArgumentCaptor<WatchlistEntity> captor = ArgumentCaptor.forClass(WatchlistEntity.class);
        when(watchlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WatchListEntityRequest req = new WatchListEntityRequest();
        req.setName("TestEntity");
        req.setAddress(null); // null address
        req.setCountry(null); // null country
        req.setRiskCategory(RiskCategory.SHELL_ENTITY);
        req.setKnownAccounts(null); // null accounts

        assertDoesNotThrow(() -> entitySenseService.createWatchListEntity(req));
        verify(watchlistRepository).save(captor.capture());
        WatchlistEntity saved = captor.getValue();
        assertEquals("TestEntity", saved.getName());
        assertNull(saved.getAddress());
        assertNull(saved.getCountry());
        assertNull(saved.getKnownAccounts());
    }
}