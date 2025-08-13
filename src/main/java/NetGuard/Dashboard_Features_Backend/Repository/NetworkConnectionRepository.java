package NetGuard.Dashboard_Features_Backend.Repository;

import NetGuard.Dashboard_Features_Backend.Entity.AvailableNetwork;
import NetGuard.Dashboard_Features_Backend.Entity.NetworkConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkConnectionRepository extends JpaRepository<NetworkConnection, Long> {

    // ==========================================
    // BASIC DEVICE QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find all connections for a specific device MAC address
     */
    List<NetworkConnection> findByDeviceMac(String deviceMac);

    /**
     * Find all currently connected devices
     */
    List<NetworkConnection> findByIsCurrentlyConnectedTrue();

    /**
     * Find active connection for a specific device
     */
    Optional<NetworkConnection> findByDeviceMacAndIsCurrentlyConnectedTrue(String deviceMac);

    /**
     * Find connections by device name
     */
    List<NetworkConnection> findByDeviceName(String deviceName);

    /**
     * Find connections by network SSID
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.network.ssid = :ssid")
    List<NetworkConnection> findByNetworkSsid(@Param("ssid") String ssid);

    // ==========================================
    // TIME-BASED QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find connections since a specific time
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.connectedAt >= :since")
    List<NetworkConnection> findConnectionsSince(@Param("since") LocalDateTime since);

    /**
     * Find connections within a time range
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.connectedAt BETWEEN :start AND :end")
    List<NetworkConnection> findConnectionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Find recent connections for a device
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.deviceMac = :deviceMac AND nc.connectedAt >= :since ORDER BY nc.connectedAt DESC")
    List<NetworkConnection> findRecentConnectionsByDevice(@Param("deviceMac") String deviceMac, @Param("since") LocalDateTime since);

    // ==========================================
    // STATISTICS AND AGGREGATION QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Count currently connected devices
     */
    @Query("SELECT COUNT(nc) FROM NetworkConnection nc WHERE nc.isCurrentlyConnected = true")
    Long countCurrentConnections();

    /**
     * Get total data usage since a specific time
     */
    @Query("SELECT COALESCE(SUM(nc.dataUsageBytes), 0) FROM NetworkConnection nc WHERE nc.connectedAt >= :since")
    Long getTotalDataUsageSince(@Param("since") LocalDateTime since);

    /**
     * Get total data usage for a specific device
     */
    @Query("SELECT COALESCE(SUM(nc.dataUsageBytes), 0) FROM NetworkConnection nc WHERE nc.deviceMac = :deviceMac")
    Long getTotalDataUsageByDevice(@Param("deviceMac") String deviceMac);

    /**
     * Get average connection duration
     */
    @Query("SELECT AVG(nc.connectionDurationMinutes) FROM NetworkConnection nc WHERE nc.connectionDurationMinutes IS NOT NULL")
    Double getAverageConnectionDuration();

    /**
     * Get total connection time for a device
     */
    @Query("SELECT COALESCE(SUM(nc.connectionDurationMinutes), 0) FROM NetworkConnection nc WHERE nc.deviceMac = :deviceMac")
    Integer getTotalConnectionTimeByDevice(@Param("deviceMac") String deviceMac);

    // ==========================================
    // NETWORK-SPECIFIC QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find active connections for a specific network
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.network.id = :networkId AND nc.isCurrentlyConnected = true")
    List<NetworkConnection> findActiveConnectionsByNetworkId(@Param("networkId") Long networkId);

    /**
     * Find connections by network BSSID
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.network.bssid = :bssid")
    List<NetworkConnection> findByNetworkBssid(@Param("bssid") String bssid);

    /**
     * Count connections per network
     */
    @Query("SELECT nc.network.ssid, COUNT(nc) FROM NetworkConnection nc GROUP BY nc.network.ssid")
    List<Object[]> countConnectionsByNetwork();

    // ==========================================
    // CONNECTION STATUS QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find connections by status
     */
    List<NetworkConnection> findByConnectionStatus(String status);

    /**
     * Find failed connections
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.connectionStatus = 'FAILED'")
    List<NetworkConnection> findFailedConnections();

    /**
     * Find long-running connections
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.isCurrentlyConnected = true AND nc.connectionDurationMinutes > :minutes")
    List<NetworkConnection> findLongRunningConnections(@Param("minutes") Integer minutes);

    // ==========================================
    // DEVICE ANALYSIS QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find most active devices by connection count
     */
    @Query("SELECT nc.deviceMac, nc.deviceName, COUNT(nc) as connectionCount FROM NetworkConnection nc GROUP BY nc.deviceMac, nc.deviceName ORDER BY connectionCount DESC")
    List<Object[]> findMostActiveDevices();

    /**
     * Find devices by data usage
     */
    @Query("SELECT nc.deviceMac, nc.deviceName, SUM(nc.dataUsageBytes) as totalData FROM NetworkConnection nc WHERE nc.dataUsageBytes IS NOT NULL GROUP BY nc.deviceMac, nc.deviceName ORDER BY totalData DESC")
    List<Object[]> findDevicesByDataUsage();

    /**
     * Find unique device MACs
     */
    @Query("SELECT DISTINCT nc.deviceMac FROM NetworkConnection nc")
    List<String> findAllDeviceMacs();

    /**
     * Find unique device names
     */
    @Query("SELECT DISTINCT nc.deviceName FROM NetworkConnection nc WHERE nc.deviceName IS NOT NULL")
    List<String> findAllDeviceNames();

    // ==========================================
    // SIMPLIFIED TIME-BASED ANALYTICS - FIXED QUERIES
    // ==========================================

    /**
     * Find connections today - SIMPLIFIED VERSION
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.connectedAt >= :startOfDay")
    List<NetworkConnection> findTodaysConnections(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Find connections this week
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.connectedAt >= :weekStart")
    List<NetworkConnection> findThisWeeksConnections(@Param("weekStart") LocalDateTime weekStart);

    // ==========================================
    // PERFORMANCE AND MONITORING - ACTIVELY USED
    // ==========================================

    /**
     * Find recently disconnected devices
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.isCurrentlyConnected = false AND nc.disconnectedAt >= :since ORDER BY nc.disconnectedAt DESC")
    List<NetworkConnection> findRecentlyDisconnected(@Param("since") LocalDateTime since);

    /**
     * Get connection statistics for dashboard
     */
    @Query("SELECT " +
            "COUNT(nc) as totalConnections, " +
            "COUNT(CASE WHEN nc.isCurrentlyConnected = true THEN 1 END) as activeConnections, " +
            "AVG(nc.connectionDurationMinutes) as avgDuration, " +
            "SUM(nc.dataUsageBytes) as totalData " +
            "FROM NetworkConnection nc")
    Object[] getConnectionStatistics();

    // ==========================================
    // SECURITY AND FILTERING - ACTIVELY USED
    // ==========================================

    /**
     * Find connections from specific IP range
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.assignedIp LIKE :ipPattern")
    List<NetworkConnection> findByIpPattern(@Param("ipPattern") String ipPattern);

    /**
     * Find connections with no data usage (potential issues)
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE (nc.dataUsageBytes IS NULL OR nc.dataUsageBytes = 0) AND nc.connectionDurationMinutes > :minDuration")
    List<NetworkConnection> findConnectionsWithNoDataUsage(@Param("minDuration") Integer minDuration);

    // ==========================================
    // CLEANUP AND MAINTENANCE - ACTIVELY USED
    // ==========================================

    /**
     * Find old disconnected connections for cleanup
     */
    @Query("SELECT nc FROM NetworkConnection nc WHERE nc.isCurrentlyConnected = false AND nc.disconnectedAt < :cutoff")
    List<NetworkConnection> findOldDisconnectedConnections(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Count connections older than specified date
     */
    @Query("SELECT COUNT(nc) FROM NetworkConnection nc WHERE nc.connectedAt < :cutoff")
    Long countOldConnections(@Param("cutoff") LocalDateTime cutoff);

    // ==========================================
    // BUSINESS LOGIC QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find devices that haven't connected recently
     */
    @Query("SELECT DISTINCT nc.deviceMac, nc.deviceName FROM NetworkConnection nc WHERE nc.deviceMac NOT IN (SELECT nc2.deviceMac FROM NetworkConnection nc2 WHERE nc2.connectedAt >= :since)")
    List<Object[]> findInactiveDevices(@Param("since") LocalDateTime since);

    /**
     * Find top data consumers
     */
    @Query("SELECT nc.deviceMac, nc.deviceName, SUM(nc.dataUsageBytes) as totalUsage FROM NetworkConnection nc WHERE nc.dataUsageBytes IS NOT NULL AND nc.connectedAt >= :since GROUP BY nc.deviceMac, nc.deviceName ORDER BY totalUsage DESC")
    List<Object[]> findTopDataConsumers(@Param("since") LocalDateTime since);

    /**
     * Find connection by IP address
     */
    Optional<NetworkConnection> findByAssignedIp(String assignedIp);

    /**
     * Find connections by network and currently connected status
     */
    List<NetworkConnection> findByNetworkAndIsCurrentlyConnectedTrue(AvailableNetwork network);

    /**
     * Find connection by IP and currently connected status
     */
    Optional<NetworkConnection> findByAssignedIpAndIsCurrentlyConnectedTrue(String assignedIp);


}