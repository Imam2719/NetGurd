package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkUsageStatsDTO {
    private Long totalBytesTransferred;
    private Long uploadBytes;
    private Long downloadBytes;
    private Integer peakDeviceCount;
    private Double averageSignalStrength;
    private List<HourlyUsageDTO> hourlyBreakdown;
    private List<DeviceUsageDTO> deviceBreakdown;
    private String timeRange;
    private LocalDateTime lastUpdated;
}