package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDTO {
    private String batchNumber;
    private LocalDate expirationDate;
    private Integer quantityOnHand;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String location;
}
