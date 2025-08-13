package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDetailsDTO {
    private ManagedDeviceDTO deviceInfo;
    private List<BrowsingHistoryDTO> recentBrowsingHistory;
    private List<ConnectionHistoryDTO> connectionHistory;
    private DeviceStatsDTO statistics;
    private List<SecurityAlertDTO> securityAlerts;
    private DeviceSettingsDTO currentSettings;
}