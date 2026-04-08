package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriticalInventoryResponse {
    private List<LowStockDTO> lowStock;
    private List<ExpiringItemDTO> expiring;
}
