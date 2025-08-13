package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceActivityDTO {
    private String deviceName;
    private String deviceMac;
    private String assignedIp;
    private String currentSite;
    private String currentActivity;
    private Integer signalStrength;
    private Long dataUsageBytes;
    private LocalDateTime lastSeen;
    private Boolean isActive;
    private String deviceType;
    private Integer batteryLevel;
}