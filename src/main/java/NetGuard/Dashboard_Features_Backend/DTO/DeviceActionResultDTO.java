package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceActionResultDTO {
    private Boolean success;
    private String message;
    private String deviceMac;
    private String deviceName;
    private String action;
    private LocalDateTime actionTime;
    private String previousStatus;
    private String newStatus;
    private String details;
}