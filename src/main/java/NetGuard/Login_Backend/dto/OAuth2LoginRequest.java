package NetGuard.Login_Backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuth2LoginRequest {
    @NotBlank(message = "OAuth2 token is required")
    private String oauth2Token;
}
