package NetGuard.Login_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    private Integer daysSinceRegistration;
    private Integer daysSinceLastLogin;
    private Boolean emailVerified;
    private Integer profileCompleteness; // Percentage
    private LocalDateTime lastPasswordUpdate;
    private String accountStatus;
    private Integer totalLogins;
    private Integer managedDevices;
    private Integer sitesMonitored;
    private Integer protectionRate;
}