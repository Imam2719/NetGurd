package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSettingsDTO {
    private Boolean contentFilterEnabled;
    private String contentFilterLevel; // STRICT, MODERATE, BASIC
    private List<String> blockedCategories;
    private List<String> allowedSites;
    private List<String> blockedSites;
    private Boolean timeLimitsEnabled;
    private TimeLimitRequestDTO timeSettings;
    private Boolean bedtimeModeEnabled;
    private String bedtimeStart;
    private String bedtimeEnd;
    private Boolean vpnRequired;
    private Boolean safeSearchEnabled;
}
