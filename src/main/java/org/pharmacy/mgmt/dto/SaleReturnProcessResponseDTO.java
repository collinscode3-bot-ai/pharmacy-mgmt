package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturnProcessResponseDTO {
    private Long returnId;
    private Long originalBillNo;
    private String patientName;
    private BigDecimal totalRefundAmount;
    private BigDecimal updatedGrandTotalAmount;
    private String returnReason;
    private LocalDateTime createdAt;
    private Integer createdBy;
    private List<ReturnProcessedItemDTO> returnedItems;
    private List<AdjustedSaleItemDTO> adjustedSaleItems;
}
