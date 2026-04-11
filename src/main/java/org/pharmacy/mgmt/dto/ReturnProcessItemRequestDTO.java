package org.pharmacy.mgmt.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pharmacy.mgmt.model.ReturnItemStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnProcessItemRequestDTO {

    @NotNull(message = "billItemId is required")
    private Integer billItemId;

    @NotNull(message = "quantityReturned is required")
    @Min(value = 1, message = "quantityReturned must be at least 1")
    private Integer quantityReturned;

    private ReturnItemStatus status;
    private String remarks;

    // Optional inventory target for restocking.
    private Integer inventoryId;
    private String batchNumber;
}
