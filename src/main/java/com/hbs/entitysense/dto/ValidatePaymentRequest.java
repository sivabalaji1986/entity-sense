package com.hbs.entitysense.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidatePaymentRequest {
    @NotBlank private String payeeName;
    private String payeeAddress;
    private String payeeCountry;
    private String accountNumber;
}