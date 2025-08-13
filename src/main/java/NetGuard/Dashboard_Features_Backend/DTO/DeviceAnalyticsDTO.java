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
    private Long totalDataUsageBytes;
    private Integer connectionDurationMinutes;
    private Integer signalStrength;
    private String currentStatus;
    private LocalDateTime lastActivity;
    private List<String> frequentSites;
    private Boolean isBlocked;
}