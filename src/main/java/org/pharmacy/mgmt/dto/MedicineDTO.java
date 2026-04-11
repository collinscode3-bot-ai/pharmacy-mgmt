package org.pharmacy.mgmt.dto;

import jakarta.validation.constraints.NotBlank;
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
public class MedicineDTO {
    private Integer medicineId;

    @NotBlank(message = "Medicine name is required")
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

    @NotNull(message = "Tax ID is required")
    private Integer taxId;

    private String taxName;

    // Optional inventory batches to create when creating/updating a medicine
    private List<InventoryDTO> inventories;
}
