package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HourlyUsageDTO {
    private Integer hour;
    private Long bytesUsed;
    private Integer deviceCount;
    private Double averageSignal;
}
