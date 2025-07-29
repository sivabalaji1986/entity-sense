package com.hbs.entitysense.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidatePaymentRequest {
    @NotBlank private String payeeName;
    private String payeeAddress;
    private String payeeCountry;
    private String accountNumber;
}