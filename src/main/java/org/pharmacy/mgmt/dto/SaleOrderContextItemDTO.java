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
public class SaleOrderContextItemDTO {
    private Integer medicineId;
    private Integer inventoryId;
    private String medicineName;
    private String dose;
    private BigDecimal unitPrice;
    private BigDecimal gstRate;
    private Boolean priceIncludesGst;
    private Integer quantity;
    private BigDecimal lineAmount;
}
