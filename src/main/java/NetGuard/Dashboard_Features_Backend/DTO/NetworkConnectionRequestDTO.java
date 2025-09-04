package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkConnectionRequestDTO {
    @NotBlank(message = "SSID is required")
    private String ssid;
    private String password;
    private String deviceName;
}
