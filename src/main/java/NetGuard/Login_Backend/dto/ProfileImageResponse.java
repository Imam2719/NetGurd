package NetGuard.Login_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageResponse {
    private String imageUrl;
    private LocalDateTime uploadedAt;
    private String message;
}
