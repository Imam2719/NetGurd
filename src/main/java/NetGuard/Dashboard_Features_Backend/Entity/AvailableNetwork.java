package NetGuard.Dashboard_Features_Backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "available_networks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableNetwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ssid;

    @Column(nullable = false)
    private String bssid; // MAC address of the access point

    @Column(nullable = false)
    private Integer signalStrength; // Signal strength in percentage

    @Column(nullable = false)
    private String frequency; // 2.4GHz or 5GHz

    @Column(nullable = false)
    private String security; // WPA2, WPA3, Open, etc.

    @Column(nullable = false)
    private Boolean isSecured;

    @Column(nullable = false)
    private String networkType; // WiFi, Cellular, Ethernet

    @Column
    private String channel;

    @Column
    private String vendor; // Router manufacturer if available

    @Column(nullable = false)
    private Boolean isConnected = false;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @Column(nullable = false)
    private LocalDateTime lastSeen;

    @Column
    private LocalDateTime firstDetected;

    @Column
    private String location; // If GPS coordinates available

    @PrePersist
    protected void onCreate() {
        if (firstDetected == null) {
            firstDetected = LocalDateTime.now();
        }
        lastSeen = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastSeen = LocalDateTime.now();
    }
}