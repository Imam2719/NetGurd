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
}