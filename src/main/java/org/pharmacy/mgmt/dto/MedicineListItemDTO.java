package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineListItemDTO {
    private Integer medicineId;
    private String productType;
    private String name;
    private String genericName;
    private String manufacturer;
    private String strength;
    private String dosageForm;
    private Integer reorderLevel;
    private Boolean isPrescriptionRequired;
    private Integer totalQuantity;
    private BigDecimal price; // representative selling price (min)
    private String status; // in_stock|low_stock|out_of_stock
}
