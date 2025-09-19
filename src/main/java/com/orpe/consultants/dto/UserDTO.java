package com.orpe.consultants.dto;

import com.orpe.consultants.model.User;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    
    private Long id;
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    private String phoneNumber;
    
    private User.Role role;
    private User.UserStatus status;
    private LocalDateTime lastLoginDate;
    private LocalDateTime createdAt;
    private String notes;
    
    
    
    // Helper methods for form binding
    public String getRoleDisplayName() {
        return role != null ? role.getDisplayName() : "";
    }
    
    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "";
    }
}
