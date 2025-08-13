package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagedDeviceDTO {
    private String deviceName;
    private String deviceMac;
    private String deviceType;
    private String assignedIp;
    private Integer signalStrength;
    private String connectionStatus;
    private Boolean isBlocked;
    private String blockReason;
    private LocalDateTime lastActivity;
    private Long dataUsageBytes;
    private Integer connectionDurationMinutes;
    private String currentSite;
    private String currentActivity;
    private Boolean hasTimeLimit;
    private Integer dailyTimeLimit; // in minutes
    private Integer timeUsedToday; // in minutes
    private LocalDateTime connectedAt;
    private LocalDateTime blockedAt;
    private List<String> recentSites;
}
