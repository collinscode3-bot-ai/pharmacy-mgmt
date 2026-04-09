package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineCatalogResponse {
    private List<MedicineListItemDTO> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
