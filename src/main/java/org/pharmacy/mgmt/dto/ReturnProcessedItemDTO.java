package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pharmacy.mgmt.model.ReturnItemStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnProcessedItemDTO {
    private Integer returnItemId;
    private Integer billItemId;
    private Integer quantityReturned;
    private BigDecimal refundLineAmount;
    private ReturnItemStatus status;
    private String remarks;
    private Integer inventoryId;
    private String batchNumber;
}
