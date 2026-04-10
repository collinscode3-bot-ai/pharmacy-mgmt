package org.pharmacy.mgmt.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationErrorDetail {
    private String field;
    private String expected;
    private String received;
    private String message;
}
