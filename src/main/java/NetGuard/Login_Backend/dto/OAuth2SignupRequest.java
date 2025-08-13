package NetGuard.Login_Backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OAuth2SignupRequest {
    @NotBlank(message = "OAuth2 token is required")
    private String oauth2Token;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Must be at least 18 years old")
    @Max(value = 120, message = "Age must be realistic")
    private Integer age;

    private Boolean acceptTerms = true;
    private Boolean acceptPrivacy = true;
}
