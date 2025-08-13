package NetGuard.Dashboard_Features_Backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// ==========================================
// NETWORK & CONNECTION DTOs
// ==========================================

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableNetworkDTO {
    private Long id;
    private String name; // SSID
    private String bssid;
    private Integer signal;
    private String frequency;
    private String security;
    private Boolean secured;
    private String networkType;
    private String channel;
    private String vendor;
    private Boolean connected;
    private Boolean available;
    private LocalDateTime lastSeen;
    private String location;
}