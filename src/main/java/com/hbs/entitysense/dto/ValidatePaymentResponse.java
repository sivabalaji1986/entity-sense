package com.hbs.entitysense.dto;

import java.util.List;
import lombok.Data;

@Data
public class ValidatePaymentResponse {
    private String status; // ALLOW or BLOCK
    private List<RiskMatchResult> possibleWatchListEntityMatches;
}