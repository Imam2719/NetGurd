package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteMonitoringDTO {
    private String deviceMac;
    private String currentUrl;
    private String pageTitle;
    private String category;
    private Boolean isSecure;
    private Boolean isBlocked;
    private LocalDateTime accessTime;
    private Integer timeOnSite;
    private String previousSite;
    private String userAgent;
    private List<String> keywords;
}