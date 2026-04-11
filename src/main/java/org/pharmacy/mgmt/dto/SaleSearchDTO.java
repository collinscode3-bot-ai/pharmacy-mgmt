package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleSearchDTO {
    private Long billNo;
    private LocalDateTime billDate;
    private String patientName;
    private BigDecimal totalAmount;
}
