package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsDTO {
    private List<DeviceAnalyticsDTO> deviceAnalytics;
    private List<DeviceActivityDTO> currentActivity;
    private List<BrowsingDataDTO> browsingData;
    private NetworkUsageStatsDTO networkUsage;
    private DashboardSummaryDTO summary;
    private LocalDateTime lastUpdated;
}
