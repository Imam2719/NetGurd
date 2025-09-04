package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkConnectionResponseDTO {
    private Boolean success;
    private String message;
    private String assignedIp;
    private Integer signalStrength;
    private LocalDateTime connectedAt;
}
