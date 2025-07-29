package com.hbs.entitysense.dto;

import com.hbs.entitysense.model.RiskCategory;
import lombok.Data;

@Data
public class RiskMatchResult {
    private Long id;
    private String name;
    private RiskCategory riskCategory;
    private boolean matchedAccount;
    private double distance;
    private String[] knownAccounts;
    private String address;
    private String country;
}
