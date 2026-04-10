package org.pharmacy.mgmt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleCreateRequest {

    private String customerName;

    private String paymentMethod;

    @Valid
    @NotEmpty(message = "At least one sale item is required")
    private List<SaleItemCreateRequest> items;

    private BigDecimal subtotalAmount;

    private BigDecimal discountAmount;

    private BigDecimal gstAmount;

    private BigDecimal grandTotalAmount;

    @Valid
    private PaymentBreakdownDTO paymentBreakdown;

    @Valid
    private List<SaleOrderContextItemDTO> orderContext;

    private String customerPhone;
}
