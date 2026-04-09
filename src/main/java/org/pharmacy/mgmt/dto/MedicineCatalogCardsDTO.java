package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineCatalogCardsDTO {
    private long totalSku;
    private long lowStockCount;
    private long outOfStockCount;
    private BigDecimal catalogValue;
}
