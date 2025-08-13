package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkPerformanceDTO {
    private String deviceMac;
    private Integer pingLatency; // in ms
    private Double downloadSpeed; // in Mbps
    private Double uploadSpeed;   // in Mbps
    private Integer packetLoss;   // percentage
    private String connectionQuality; // EXCELLENT, GOOD, FAIR, POOR
    private LocalDateTime measuredAt;
    private String serverUsed;
}
