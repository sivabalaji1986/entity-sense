package com.hbs.entitysense.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbs.entitysense.dto.*;
import com.hbs.entitysense.entity.WatchlistEntity;
import com.hbs.entitysense.repository.WatchlistRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hbs.entitysense.constants.EntitySenseConstant.*;

@Service
@RequiredArgsConstructor
public class EntitySenseService {

    private final HttpClient httpClient;
    private final WatchlistRepository repository;
    private final ObjectMapper objectMapper;

    public void createWatchListEntity(CreateWatchListEntityRequest request) {
        float[] embedding = generateEmbedding(request.getName(), request.getAddress(), request.getCountry());
        WatchlistEntity entity = new WatchlistEntity();
        entity.setName(request.getName());
        entity.setAddress(request.getAddress());
        entity.setCountry(request.getCountry());
        entity.setKnownAccounts(request.getKnownAccounts());
        entity.setRiskCategory(request.getRiskCategory());
        entity.setEmbedding(embedding);
        repository.save(entity);
    }

    public ValidatePaymentResponse validatePayment(ValidatePaymentRequest request) {
        float[] inputEmbedding = generateEmbedding(request.getPayeeName(), request.getPayeeAddress(), request.getPayeeCountry());

        List<RiskMatchResult> matches = repository.findAll().stream()
                .map(entity -> {
                    double distance = cosineDistance(inputEmbedding, entity.getEmbedding());
                    boolean accountMatch = entity.getKnownAccounts() != null && entity.getKnownAccounts().contains(request.getAccountNumber());
                    RiskMatchResult result = new RiskMatchResult();
                    result.setId(entity.getId());
                    result.setName(entity.getName());
                    result.setRiskCategory(entity.getRiskCategory());
                    result.setDistance(distance);
                    result.setMatchedAccount(accountMatch);
                    return result;
                })
                .filter(result -> result.getDistance() < 0.3 || result.isMatchedAccount())
                .sorted(Comparator.comparingDouble(RiskMatchResult::getDistance))
                .collect(Collectors.toList());

        ValidatePaymentResponse response = new ValidatePaymentResponse();
        response.setPossibleWatchListEntityMatches(matches);
        response.setStatus(matches.isEmpty() ? "ALLOW" : "BLOCK");
        return response;
    }

    private float[] generateEmbedding(String name, String address, String country) {
        try {
            String text = String.join(" ‖ ", name, address != null ? address : "", country != null ? country : "");
            Map<String, Object> body = Map.of(OLLAMA_EMBEDDINGS_REQ_MODEL_KEY, OLLAMA_EMBEDDINGS_REQ_MODEL_VALUE, OLLAMA_EMBEDDINGS_REQ_PROMPT_KEY, text);
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            OllamaEmbeddingResponse embResp = objectMapper.readValue(response.body(), OllamaEmbeddingResponse.class);
            List<Float> list = embResp.getEmbedding();
            if (list == null) return null;
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
            return arr;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to generate embedding", ex);
        }
    }

    @Data
    @AllArgsConstructor
    static class OllamaEmbeddingRequest {
        private String model;
        private String prompt;
    }

    private double cosineDistance(float[] a, float[] b) {
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += Math.pow(a[i], 2);
            normB += Math.pow(b[i], 2);
        }
        return 1 - (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

}