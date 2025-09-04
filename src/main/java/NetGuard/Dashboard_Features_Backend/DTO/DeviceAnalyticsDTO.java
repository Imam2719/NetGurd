package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAnalyticsDTO {
    private String deviceName;
    private String deviceMac;
    private String deviceType;
    private Long totalDataUsage;
    private Integer totalDuration;
    private Integer signalStrength;
    private String connectionStatus;
    private LocalDateTime lastActive;
    private List<String> frequentSites;
    private Boolean isBlocked;

    // Alias method for lastActive
    public LocalDateTime getLastActive() {
        return this.lastActive;
    }
}