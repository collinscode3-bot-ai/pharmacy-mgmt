package org.pharmacy.mgmt.dto;

import lombok.Builder;
import lombok.Data;
import org.pharmacy.mgmt.model.Role;

@Data
@Builder
public class UserResponseDTO {
    private String username;
    private String full_name;
    private Role role;
}