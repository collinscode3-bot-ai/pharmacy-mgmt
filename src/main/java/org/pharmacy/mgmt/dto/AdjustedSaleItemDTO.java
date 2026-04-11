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
public class AdjustedSaleItemDTO {
    private Integer billItemId;
    private Integer medicineId;
    private String medicineName;
    private Integer quantitySold;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
}
