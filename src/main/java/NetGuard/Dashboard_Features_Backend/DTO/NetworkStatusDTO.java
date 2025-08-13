package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkStatusDTO {
    private String networkName;
    private String networkBssid;
    private Boolean isConnected;
    private Integer signalStrength;
    private String frequency;
    private String securityType;
    private String ipAddress;
    private String gateway;
    private String dns;
    private Long bytesReceived;
    private Long bytesSent;
    private Integer packetsReceived;
    private Integer packetsSent;
    private LocalDateTime lastUpdate;
}