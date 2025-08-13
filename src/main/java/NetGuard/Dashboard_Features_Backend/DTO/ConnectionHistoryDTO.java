package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionHistoryDTO {
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
    private String networkName;
    private String networkBssid;
    private Integer signalStrength;
    private Long dataUsed;
    private Integer durationMinutes;
    private String connectionType; // WIFI, ETHERNET, MOBILE
    private String ipAddress;
    private String disconnectReason;
}
