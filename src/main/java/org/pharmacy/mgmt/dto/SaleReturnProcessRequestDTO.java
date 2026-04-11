package org.pharmacy.mgmt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturnProcessRequestDTO {

    @NotNull(message = "originalBillNo is required")
    private Long originalBillNo;

    private String returnReason;

    @Valid
    @NotEmpty(message = "At least one returned line item is required")
    private List<ReturnProcessItemRequestDTO> items;
}
