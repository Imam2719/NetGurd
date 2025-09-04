package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkStatsDTO {
    private Double totalDataUsageGB;
    private Integer totalConnectedDevices;
    private Double averageSignalStrength;
    private String primaryFrequency;
    private Integer securedNetworksCount;
    private Integer openNetworksCount;
}

