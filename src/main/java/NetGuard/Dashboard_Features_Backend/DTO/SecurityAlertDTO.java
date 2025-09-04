package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlertDTO {
    private String alertType;
    private String severity;
    private String description;
    private String url;
    private LocalDateTime timestamp;
    private String status;
    private Boolean resolved;
    private String details;
}
