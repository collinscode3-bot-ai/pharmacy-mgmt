package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentBreakdownDTO {
    private BigDecimal cash;
    private BigDecimal card;
    private BigDecimal upi;
    private BigDecimal other;
}
