package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HourlyActivityDTO {
    private Integer hour;
    private Integer activeMinutes;
    private Long dataUsed;
    private Integer sitesVisited;
    private String primaryActivity;
}