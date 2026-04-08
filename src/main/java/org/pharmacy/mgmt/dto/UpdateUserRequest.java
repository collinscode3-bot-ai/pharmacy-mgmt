package org.pharmacy.mgmt.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.pharmacy.mgmt.model.Role;

@Data
public class UpdateUserRequest {

    // In this app, username is used as login email.
    @Size(min = 3, max = 50)
    private String username;

    @Size(max = 100)
    private String full_name;

    private Role role;

    @Size(min = 6, max = 100)
    private String password;
}