package NetGuard.Login_Backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username/Email is required")
    @Email(message = "Please provide a valid email")
    private String username; // Actually email as per frontend

    @NotBlank(message = "Password is required")
    private String password;
}