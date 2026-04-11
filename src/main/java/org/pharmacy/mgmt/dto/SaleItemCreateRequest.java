package org.pharmacy.mgmt.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItemCreateRequest {

    private Integer billItemId;

    @NotNull(message = "Medicine ID is required")
    private Integer medicineId;

    private Integer inventoryId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // price per unit sent by client for computation and reconciliation
    private BigDecimal unitPrice;

    // GST rate as percentage (e.g. 5, 12, 18)
    private BigDecimal gstRate;

    // true if unitPrice includes GST
    private Boolean priceIncludesGst;

    // optional client values for reconciliation against server-computed values
    private BigDecimal lineAmount;
}
