package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pharmacy.mgmt.model.PaymentType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentBreakdownResponse {

    private Integer paymentId;
    private PaymentType paymentType;
    private BigDecimal amount;
}
