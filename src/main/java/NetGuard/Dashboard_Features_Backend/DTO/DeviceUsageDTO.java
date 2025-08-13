package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceUsageDTO {
    private String deviceName;
    private String deviceMac;
    private Long bytesUsed;
    private Integer connectionMinutes;
    private Double dataPercentage;
}