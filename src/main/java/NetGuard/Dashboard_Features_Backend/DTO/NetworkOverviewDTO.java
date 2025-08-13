package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkOverviewDTO {
    private String connectedWifi;
    private Integer totalDevices;
    private Integer activeDevices;
    private String totalTimeUsed;
    private Boolean vpnActive;
    private Integer dailyVisitedSites;
    private List<AvailableNetworkDTO> availableNetworks;
    private List<ConnectedDeviceDTO> connectedDevices;
    private NetworkStatsDTO networkStats;
}