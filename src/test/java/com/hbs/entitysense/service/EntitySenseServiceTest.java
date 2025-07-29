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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;

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

    /** Utility to stub Ollama response with given list */
    private void stubOllamaResponse(List<Float> embeddingList) {
        RequestBodyUriSpec requestBodyUriSpec = mock(RequestBodyUriSpec.class);
        RequestHeadersSpec<?> requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);
        OllamaEmbeddingResponse mockResponse = new OllamaEmbeddingResponse();
        mockResponse.setEmbedding(embeddingList);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(eq(HttpHeaders.CONTENT_TYPE), eq(MediaType.APPLICATION_JSON_VALUE))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenAnswer(inv -> requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OllamaEmbeddingResponse.class)).thenReturn(Mono.just(mockResponse));
    }

    @Test
    public void testValidatePaymentReturnsAllowWhenNoMatch() {
        ValidatePaymentRequest request = new ValidatePaymentRequest("John Doe", "123 Main St", "USA", "ACCT001");
        // empty embedding vector
        List<Float> emptyVec = new ArrayList<>(Collections.nCopies(768, 0.0f));
        stubOllamaResponse(emptyVec);
        when(watchlistRepository.findAll()).thenReturn(Collections.emptyList());

        ValidatePaymentResponse response = entitySenseService.validatePayment(request);
        assertEquals("ALLOW", response.getStatus());
        assertTrue(response.getPossibleWatchListEntityMatches().isEmpty());
    }

    @Test
    public void testValidatePaymentReturnsBlockWhenDistanceMatch() {
        ValidatePaymentRequest request = new ValidatePaymentRequest("Alice", "Addr", "US", "AC123");
        // embedding identical to entity will give distance = 0
        List<Float> vec = new ArrayList<>(Collections.nCopies(768, 1.0f));
        stubOllamaResponse(vec);

        WatchlistEntity entity = new WatchlistEntity();
        entity.setId(1L);
        entity.setName("Alice");
        entity.setAddress("Addr");
        entity.setCountry("US");
        entity.setKnownAccounts(null);
        entity.setRiskCategory(RiskCategory.SANCTION);
        entity.setEmbedding(convertListToArray(vec));

        when(watchlistRepository.findAll()).thenReturn(List.of(entity));

        ValidatePaymentResponse response = entitySenseService.validatePayment(request);
        assertEquals("BLOCK", response.getStatus());
        assertEquals(1, response.getPossibleWatchListEntityMatches().size());
    }

    @Test
    public void testValidatePaymentReturnsBlockWhenAccountMatchOnly() {
        ValidatePaymentRequest request = new ValidatePaymentRequest("Bob", "Addr2", "US", "AC999");
        // use different embedding, but account match triggers block
        List<Float> diffVec = new ArrayList<>(Collections.nCopies(768, 0.5f));
        stubOllamaResponse(diffVec);

        WatchlistEntity entity = new WatchlistEntity();
        entity.setId(2L);
        entity.setName("Bob");
        entity.setAddress("Addr2");
        entity.setCountry("US");
        entity.setKnownAccounts(List.of("AC999"));
        entity.setRiskCategory(RiskCategory.MULE);
        entity.setEmbedding(new float[768]); // different vector

        when(watchlistRepository.findAll()).thenReturn(List.of(entity));

        ValidatePaymentResponse response = entitySenseService.validatePayment(request);
        assertEquals("BLOCK", response.getStatus());
        assertFalse(response.getPossibleWatchListEntityMatches().isEmpty());
    }

    @Test
    public void testCreateWatchListEntityThrowsOnNullEmbedding() {
        CreateWatchListEntityRequest req = new CreateWatchListEntityRequest();
        req.setName("NullEmb");
        req.setAddress("X");
        req.setCountry("Y");
        req.setRiskCategory(RiskCategory.PEP);
        req.setKnownAccounts(List.of("AC1"));
        // stub response with null embedding
        RequestBodyUriSpec uriSpec = mock(RequestBodyUriSpec.class);
        RequestHeadersSpec<?> headersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec respSpec = mock(ResponseSpec.class);
        OllamaEmbeddingResponse nullResp = new OllamaEmbeddingResponse();
        nullResp.setEmbedding(null);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.header(anyString(), anyString())).thenReturn(uriSpec);
        when(uriSpec.bodyValue(any())).thenAnswer(inv -> headersSpec);
        when(headersSpec.retrieve()).thenReturn(respSpec);
        when(respSpec.bodyToMono(OllamaEmbeddingResponse.class)).thenReturn(Mono.just(nullResp));

        assertThrows(IllegalStateException.class,
                () -> entitySenseService.createWatchListEntity(req));
    }

    @Test
    public void testCosineDistanceIdenticalVectors() throws Exception {
        // Use reflection to access private cosineDistance
        Method m = EntitySenseService.class.getDeclaredMethod("cosineDistance", float[].class, float[].class);
        m.setAccessible(true);
        float[] a = new float[] {1.0f, 0.0f, 0.0f};
        float[] b = new float[] {1.0f, 0.0f, 0.0f};
        double dist = (double) m.invoke(entitySenseService, a, b);
        assertEquals(0.0, dist, 1e-6);
    }

    @Test
    public void testCreateWatchListEntitySuccess() {
        // 1) Prepare request
        CreateWatchListEntityRequest request = new CreateWatchListEntityRequest();
        request.setName("Good Actor");
        request.setAddress("100 Safe Road");
        request.setCountry("US");
        request.setRiskCategory(RiskCategory.CYBER_THREAT);
        request.setKnownAccounts(List.of("ACC123"));

        // 2) Stub Ollama to return a non-null embedding
        List<Float> embeddingList = new ArrayList<>(Collections.nCopies(768, 0.42f));
        stubOllamaResponse(embeddingList);  // use your existing stubOllamaResponse helper

        // 3) Capture the entity passed to save()
        ArgumentCaptor<WatchlistEntity> captor = ArgumentCaptor.forClass(WatchlistEntity.class);
        when(watchlistRepository.save(any(WatchlistEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 4) Execute
        assertDoesNotThrow(() -> entitySenseService.createWatchListEntity(request));

        // 5) Verify & assert
        verify(watchlistRepository).save(captor.capture());
        WatchlistEntity saved = captor.getValue();
        assertEquals("Good Actor", saved.getName());
        assertEquals("100 Safe Road", saved.getAddress());
        assertEquals("US", saved.getCountry());
        assertEquals(RiskCategory.CYBER_THREAT, saved.getRiskCategory());
        assertNotNull(saved.getEmbedding());
        assertEquals(768, saved.getEmbedding().length);
        // ensure embedding was set from stub
        assertEquals(0.42f, saved.getEmbedding()[0], 1e-6f);
    }


    // Helper to convert List<Float> to float[]
    private float[] convertListToArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}