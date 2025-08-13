package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUsageDTO {
    private String category;
    private Integer sitesVisited;
    private Integer timeSpentMinutes;
    private Double percentage;
    private List<String> topSites;
}
