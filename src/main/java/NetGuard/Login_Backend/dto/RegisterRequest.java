package NetGuard.Login_Backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Must be at least 18 years old")
    @Max(value = 120, message = "Age must be realistic")
    private Integer age;

    @NotBlank(message = "Password is required")
    @Size(min = 4, max = 12, message = "Password must be between 4 and 12 characters")
    private String password;

    // Optional fields for enhanced functionality
    private Boolean acceptTerms;
    private Boolean acceptPrivacy;
}