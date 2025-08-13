package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkConnectionRequestDTO {
    private String ssid;
    private String password;
    private String deviceName;
    private String deviceMac;
}
