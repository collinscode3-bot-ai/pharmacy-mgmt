package org.pharmacy.mgmt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String full_name;

    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;
}
