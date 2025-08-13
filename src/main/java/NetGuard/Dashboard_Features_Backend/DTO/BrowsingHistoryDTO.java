package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrowsingHistoryDTO {
    private String url;
    private String domain;
    private String title;
    private String category;
    private LocalDateTime visitTime;
    private Integer durationMinutes;
    private Boolean isSecure;
    private Boolean isBlocked;
    private String browserType;
    private Integer tabsOpen;
    private String userAgent;
}