package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleInventoryInfoDTO {

    private Integer inventoryId;
    private String batchNumber;
    private LocalDate expirationDate;
    private String location;
    private Integer availableQuantity;
}
