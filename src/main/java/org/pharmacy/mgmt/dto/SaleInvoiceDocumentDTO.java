package org.pharmacy.mgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleInvoiceDocumentDTO {
    private String fileName;
    private String contentType;
    private String base64Content;
    private String archivePath;
}
