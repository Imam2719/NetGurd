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
    private Integer pingLatency;
    private Double downloadSpeed;
    private Double uploadSpeed;
    private Integer packetLoss;
    private String quality;
    private LocalDateTime timestamp;
    private String testType;
}
