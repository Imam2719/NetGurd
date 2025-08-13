package NetGuard.Login_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String name;
    private String email;
    private String profileImageUrl;
    private Boolean emailVerified;
    private Long expiresIn; // Token expiration time in milliseconds
}