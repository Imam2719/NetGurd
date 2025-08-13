package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private Integer totalDevices;
    private Integer activeDevices;
    private Integer blockedDevices;
    private Long totalDataUsageGB;
    private Integer averageSignalStrength;
    private String mostUsedSite;
    private String peakUsageHour;
    private Boolean allDevicesSecure;
}