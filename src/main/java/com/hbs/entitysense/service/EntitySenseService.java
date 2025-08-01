package com.hbs.entitysense.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbs.entitysense.dto.*;
import com.hbs.entitysense.entity.WatchlistEntity;
import com.hbs.entitysense.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hbs.entitysense.constants.EntitySenseConstant.*;

@Service
@RequiredArgsConstructor
public class EntitySenseService {

    private final HttpClient httpClient;
    private final WatchlistRepository watchlistRepository;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(EntitySenseService.class);

    public void createWatchListEntity(WatchListEntityRequest request) {
        logger.info("Creating watch list entity: {}", request);
        float[] embedding = generateEmbedding(request.getName(), request.getAddress(), request.getCountry());
        logger.info("Generated embedding for entity {}: {}", request.getName(), embedding);
        if (embedding == null) {
            logger.error("Failed to generate embedding for entity: {}", request.getName());
            throw new RuntimeException("Failed to generate embedding for entity: " + request.getName());
        }

        WatchlistEntity entity = new WatchlistEntity();
        entity.setName(request.getName());
        entity.setAddress(request.getAddress());
        entity.setCountry(request.getCountry());
        entity.setKnownAccounts( (request.getKnownAccounts()));
        entity.setRiskCategory(request.getRiskCategory());
        entity.setEmbedding(embedding);
        watchlistRepository.save(entity);
    }

    public ValidatePaymentResponse validatePayment(ValidatePaymentRequest request) {
        logger.info("Validating payment for request: {}", request);
        float[] inputEmbedding = generateEmbedding(request.getPayeeName(), request.getPayeeAddress(), request.getPayeeCountry());
        if(inputEmbedding == null) {
            logger.error("Failed to generate embedding for payee: {}", request.getPayeeName());
            throw new RuntimeException("Failed to generate embedding for payee: " + request.getPayeeName());
        }
        logger.info("Generated embedding for payee {}: {}", request.getPayeeName(), Arrays.toString(inputEmbedding));

        List<RiskMatchResult> matches = watchlistRepository.findAll().stream()
                .map(entity -> {
                    double distance = cosineDistance(inputEmbedding, entity.getEmbedding());
                    boolean accountMatch = entity.getKnownAccounts() != null && Arrays.asList(entity.getKnownAccounts()).contains(request.getAccountNumber());
                    RiskMatchResult result = new RiskMatchResult();
                    result.setId(entity.getId());
                    result.setName(entity.getName());
                    result.setRiskCategory(entity.getRiskCategory());
                    result.setDistance(distance);
                    result.setMatchedAccount(accountMatch);
                    result.setAddress(entity.getAddress());
                    result.setCountry(entity.getCountry());
                    result.setKnownAccounts(entity.getKnownAccounts());
                    return result;
                })
                .filter(result -> result.getDistance() < ENTITY_SENSE_COSINE_DISTANCE_THRESHOLD || result.isMatchedAccount())
                .sorted(Comparator.comparingDouble(RiskMatchResult::getDistance))
                .collect(Collectors.toList());

        logger.info("Found {} potential matches for payee {}", matches.size(), request.getPayeeName());
        ValidatePaymentResponse response = new ValidatePaymentResponse();
        response.setPossibleWatchListEntityMatches(matches);
        response.setStatus(matches.isEmpty() ? PAYMENT_STATUS_ALLOW : PAYMENT_STATUS_BLOCK);
        if (!matches.isEmpty()) {
            logger.error("Payment blocked due to potential match with watch list entities {}", response);
        } else {
            logger.info("Payment allowed, no matches found {}", response);
        }
        return response;
    }

    private float[] generateEmbedding(String name, String address, String country) {
        try {
            String text = String.join(" ‖ ", name, address != null ? address : "", country != null ? country : "");
            logger.info("Generating embedding for text: {}", text);
            Map<String, Object> body = Map.of(OLLAMA_EMBEDDINGS_REQ_MODEL_KEY, OLLAMA_EMBEDDINGS_REQ_MODEL_VALUE, OLLAMA_EMBEDDINGS_REQ_PROMPT_KEY, text);
            logger.info("Request body for Ollama embedding: {}", body);
            String json = objectMapper.writeValueAsString(body);
            logger.info("JSON request for Ollama embedding: {}", json);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL+OLLAMA_EMBEDDINGS_URL))
                    .timeout(Duration.ofSeconds(OLLAMA_TIMEOUT_VALUE))
                    .header(OLLAMA_CONTENT_TYPE_KEY, OLLAMA_CONTENT_TYPE_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Received response from Ollama: {}", response.body());
            OllamaEmbeddingResponse ollamaEmbeddingResponse = objectMapper.readValue(response.body(), OllamaEmbeddingResponse.class);
            logger.info("Parsed Ollama embedding response: {}", ollamaEmbeddingResponse);
            List<Float> list = ollamaEmbeddingResponse.getEmbedding();
            if (list == null) return null;
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
            return arr;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("Error generating embedding - {}", ex.getMessage());
            throw new RuntimeException("Failed to generate embedding", ex);
        }
    }

    private double cosineDistance(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Invalid vectors");
        }

        double dotProduct = 0.0, normA = 0.0, normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i]; // → A ⋅ B
            normA += Math.pow(vectorA[i], 2); // → ||A||²
            normB += Math.pow(vectorB[i], 2); // → ||B||²
        }

        // Avoid division by zero if embedding is invalid, set maximum distance
        if (normA == 0.0 || normB == 0.0) return 1.0;

        // EntitySense uses cosine Distance (1 - cosine similarity)
        // Formula: 1 - (A⋅B)/(||A|| × ||B||)
        return 1 - (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

}