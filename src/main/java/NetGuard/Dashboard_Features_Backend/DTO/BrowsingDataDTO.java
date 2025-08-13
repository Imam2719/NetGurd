package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrowsingDataDTO {
    private String deviceName;
    private String deviceMac;
    private String currentSite;
    private String siteCategory;
    private LocalDateTime lastAccess;
    private Long timeSpentMinutes;
    private Boolean isSecure;
    private Boolean vpnActive;
    private String browserType;
    private Integer tabs;
}
