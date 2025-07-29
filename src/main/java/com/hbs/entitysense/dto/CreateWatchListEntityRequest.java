package com.hbs.entitysense.dto;

import com.hbs.entitysense.model.RiskCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateWatchListEntityRequest {
    @NotBlank private String name;
    private String address;
    private String country;
    private String[] knownAccounts;
    private RiskCategory riskCategory;
}