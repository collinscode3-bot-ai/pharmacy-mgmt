package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpiringItemDTO {
    private String medicineName;
    private String batchNumber;
    private LocalDate expirationDate;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String location;
}
