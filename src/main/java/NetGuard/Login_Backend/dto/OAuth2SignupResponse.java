package NetGuard.Login_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2SignupResponse {
    private String token; // JWT token
    private String name;
    private String email;
    private String profileImageUrl;
    private String temporaryPassword;
    private String message;
}