package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeLimitRequestDTO {
    private Integer dailyLimitMinutes;
    private String startTime; // e.g., "08:00"
    private String endTime;   // e.g., "20:00"
    private List<String> allowedDays; // e.g., ["MONDAY", "TUESDAY", ...]
    private Boolean enabled;
    private List<String> exemptSites; // Sites that don't count toward time limit
}