package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineDTO {
    private Integer medicineId;
    private String name;
    private String genericName;
    private String manufacturer;
    private String strength;
    private String dosageForm;
    private Integer reorderLevel;
    private Boolean isPrescriptionRequired;
    private String description;
    private Integer taxId;
    private String taxName;
}
