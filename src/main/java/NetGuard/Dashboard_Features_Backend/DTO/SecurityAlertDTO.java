package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlertDTO {
    private String alertType; // MALWARE, PHISHING, INAPPROPRIATE_CONTENT, etc.
    private String severity;  // LOW, MEDIUM, HIGH, CRITICAL
    private String description;
    private String url;
    private LocalDateTime detectedAt;
    private String action; // BLOCKED, WARNED, ALLOWED
    private Boolean resolved;
    private String details;
}
