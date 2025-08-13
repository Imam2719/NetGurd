package NetGuard.Dashboard_Features_Backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "network_connections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private AvailableNetwork network;

    @Column(nullable = false)
    private String deviceName;

    @Column(nullable = false)
    private String deviceMac;

    @Column
    private String assignedIp;

    @Column(nullable = false)
    private LocalDateTime connectedAt;

    @Column
    private LocalDateTime disconnectedAt;

    @Column(nullable = false)
    private Boolean isCurrentlyConnected = true;

    @Column
    private Long dataUsageBytes = 0L;

    @Column
    private Integer connectionDurationMinutes = 0;

    @Column
    private String connectionStatus; // CONNECTED, CONNECTING, DISCONNECTED, FAILED

    @Column
    private String disconnectionReason;

    @PrePersist
    protected void onCreate() {
        if (connectedAt == null) {
            connectedAt = LocalDateTime.now();
        }
        if (connectionStatus == null) {
            connectionStatus = "CONNECTING";
        }
    }
}