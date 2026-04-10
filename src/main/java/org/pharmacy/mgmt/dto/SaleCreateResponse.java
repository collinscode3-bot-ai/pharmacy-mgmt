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
public class SaleCreateResponse {

    private Long saleId;
    private Long billNo;
    private String customerName;
    private String paymentMethod;
    private List<SaleItemCreateRequest> items;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal gstAmount;
    private BigDecimal grandTotalAmount;
    private PaymentBreakdownDTO paymentBreakdown;
    private SaleInvoiceDocumentDTO invoiceDocument;
    private List<SaleOrderContextItemDTO> orderContext;
    private String customerPhone;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer createdBy;
    private Boolean isActive;
}
