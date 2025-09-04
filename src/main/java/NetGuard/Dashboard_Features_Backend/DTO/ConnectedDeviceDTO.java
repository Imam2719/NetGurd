package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectedDeviceDTO {
    private String deviceName;
    private String deviceMac;
    private String assignedIp;
    private LocalDateTime connectedAt;
    private Long dataUsageBytes;
    private String connectionStatus;
    private Integer signalStrength;
    private String deviceType;
    private Integer connectionDurationMinutes;

    // Constructor for backwards compatibility
    public ConnectedDeviceDTO(String deviceName, String deviceMac, String assignedIp,
                              LocalDateTime connectedAt, Long dataUsageBytes,
                              String connectionStatus, Integer signalStrength) {
        this.deviceName = deviceName;
        this.deviceMac = deviceMac;
        this.assignedIp = assignedIp;
        this.connectedAt = connectedAt;
        this.dataUsageBytes = dataUsageBytes;
        this.connectionStatus = connectionStatus;
        this.signalStrength = signalStrength;
        this.deviceType = "unknown";
        this.connectionDurationMinutes = 0;
    }

    // Alias method for getDataUsageBytes
    public Long getDataUsage() {
        return this.dataUsageBytes;
    }
}