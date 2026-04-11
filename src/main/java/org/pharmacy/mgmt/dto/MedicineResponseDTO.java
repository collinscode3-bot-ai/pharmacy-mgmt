package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import org.pharmacy.mgmt.dto.InventoryDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineResponseDTO {
    private Integer medicineId;
    private String name;
    private String productType;
    private String genericName;
    private String manufacturer;
    private String strength;
    private String dosageForm;
    private Integer reorderLevel;
    private Boolean isPrescriptionRequired;
    private Boolean isActive;
    private String description;
    private Integer taxId;
    private String taxName;
    private BigDecimal taxPercentage;
    private List<InventoryDTO> inventories;
}
