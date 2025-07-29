package com.hbs.entitysense.service;

import com.hbs.entitysense.dto.*;
import com.hbs.entitysense.entity.WatchlistEntity;
import com.hbs.entitysense.repository.WatchlistEntityRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntitySenseService {

    private final WebClient ollamaClient;
    private final WatchlistEntityRepository repository;

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
        String prompt = String.join(" ‖ ", name, address != null ? address : "", country != null ? country : "");
        EmbeddingResponse response = ollamaClient.post()
                .uri("/api/embeddings")
                .bodyValue(new OllamaEmbeddingRequest("nomic-embed-text", prompt))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to fetch embedding")))
                .block();
        return response != null ? response.getEmbedding() : new float[768];
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