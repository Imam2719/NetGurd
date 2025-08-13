package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatsDTO {
    private Long totalDataUsage;
    private Integer totalConnectionTime;
    private Integer averageSessionDuration;
    private Integer totalSitesVisited;
    private String mostVisitedSite;
    private String mostUsedCategory;
    private Double averageSignalStrength;
    private Integer totalConnections;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private List<CategoryUsageDTO> categoryBreakdown;
    private List<HourlyActivityDTO> activityPattern;
}