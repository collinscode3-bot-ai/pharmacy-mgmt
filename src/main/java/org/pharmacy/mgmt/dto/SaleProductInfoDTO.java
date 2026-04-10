package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleProductInfoDTO {

    private Integer medicineId;
    private String name;
    private String productType;
    private String genericName;
    private String manufacturer;
    private String strength;
    private String dosageForm;
}
