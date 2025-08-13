package NetGuard.Dashboard_Features_Backend.Repository;

import NetGuard.Dashboard_Features_Backend.Entity.AvailableNetwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailableNetworkRepository extends JpaRepository<AvailableNetwork, Long> {

    // ==========================================
    // BASIC NETWORK QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find network by SSID (network name)
     */
    Optional<AvailableNetwork> findBySsid(String ssid);

    /**
     * Find network by BSSID (MAC address)
     */
    Optional<AvailableNetwork> findByBssid(String bssid);

    /**
     * Find all available networks
     */
    List<AvailableNetwork> findByIsAvailableTrue();

    /**
     * Find all connected networks
     */
    List<AvailableNetwork> findByIsConnectedTrue();

    /**
     * Find networks by type (WiFi, Cellular, etc.)
     */
    List<AvailableNetwork> findByNetworkType(String networkType);

    // ==========================================
    // SECURITY-BASED QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find all secured networks
     */
    List<AvailableNetwork> findByIsSecuredTrue();

    /**
     * Find all open networks
     */
    List<AvailableNetwork> findByIsSecuredFalse();

    /**
     * Find networks by security type
     */
    List<AvailableNetwork> findBySecurity(String security);

    /**
     * Find networks with specific security protocols
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.security LIKE %:securityType%")
    List<AvailableNetwork> findBySecurityContaining(@Param("securityType") String securityType);

    // ==========================================
    // SIGNAL STRENGTH QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find networks with strong signal (above threshold)
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.signalStrength >= :minSignal ORDER BY an.signalStrength DESC")
    List<AvailableNetwork> findBySignalStrengthGreaterThanEqual(@Param("minSignal") Integer minSignal);

    /**
     * Find networks with weak signal (below threshold)
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.signalStrength < :maxSignal ORDER BY an.signalStrength ASC")
    List<AvailableNetwork> findBySignalStrengthLessThan(@Param("maxSignal") Integer maxSignal);

    /**
     * Find networks ordered by signal strength
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.isAvailable = true ORDER BY an.signalStrength DESC")
    List<AvailableNetwork> findAllBySignalStrengthDesc();

    // ==========================================
    // TIME-BASED QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find recently seen networks
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.lastSeen >= :since")
    List<AvailableNetwork> findRecentlySeenNetworks(@Param("since") LocalDateTime since);

    /**
     * Find networks first detected since a specific time
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.firstDetected >= :since")
    List<AvailableNetwork> findRecentlyDiscovered(@Param("since") LocalDateTime since);

    /**
     * Find networks not seen recently (potentially offline)
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.lastSeen < :cutoff AND an.isAvailable = true")
    List<AvailableNetwork> findStaleNetworks(@Param("cutoff") LocalDateTime cutoff);

    // ==========================================
    // FREQUENCY AND CHANNEL QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find networks by frequency band
     */
    List<AvailableNetwork> findByFrequency(String frequency);

    /**
     * Find 2.4GHz networks
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.frequency = '2.4GHz'")
    List<AvailableNetwork> find2point4GHzNetworks();

    /**
     * Find 5GHz networks
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.frequency = '5GHz'")
    List<AvailableNetwork> find5GHzNetworks();

    /**
     * Find networks by channel
     */
    List<AvailableNetwork> findByChannel(String channel);

    // ==========================================
    // LOCATION-BASED QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find networks by location
     */
    List<AvailableNetwork> findByLocation(String location);

    /**
     * Find networks with location data
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.location IS NOT NULL AND an.location != ''")
    List<AvailableNetwork> findNetworksWithLocation();

    // ==========================================
    // VENDOR AND MANUFACTURER QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find networks by vendor/manufacturer
     */
    List<AvailableNetwork> findByVendor(String vendor);

    /**
     * Find networks with known vendors
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.vendor IS NOT NULL AND an.vendor != '' AND an.vendor != 'Unknown'")
    List<AvailableNetwork> findNetworksWithKnownVendors();

    // ==========================================
    // STATISTICAL QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Count secured networks
     */
    @Query("SELECT COUNT(an) FROM AvailableNetwork an WHERE an.isSecured = true")
    Long countSecuredNetworks();

    /**
     * Count open networks
     */
    @Query("SELECT COUNT(an) FROM AvailableNetwork an WHERE an.isSecured = false")
    Long countOpenNetworks();

    /**
     * Get average signal strength
     */
    @Query("SELECT AVG(an.signalStrength) FROM AvailableNetwork an WHERE an.isAvailable = true")
    Double getAverageSignalStrength();

    /**
     * Get maximum signal strength
     */
    @Query("SELECT MAX(an.signalStrength) FROM AvailableNetwork an WHERE an.isAvailable = true")
    Integer getMaxSignalStrength();

    /**
     * Get minimum signal strength
     */
    @Query("SELECT MIN(an.signalStrength) FROM AvailableNetwork an WHERE an.isAvailable = true")
    Integer getMinSignalStrength();

    /**
     * Count networks by frequency
     */
    @Query("SELECT an.frequency, COUNT(an) FROM AvailableNetwork an GROUP BY an.frequency")
    List<Object[]> countByFrequency();

    /**
     * Count networks by security type
     */
    @Query("SELECT an.security, COUNT(an) FROM AvailableNetwork an GROUP BY an.security")
    List<Object[]> countBySecurity();

    // ==========================================
    // SEARCH AND FILTERING QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Search networks by SSID pattern
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.ssid LIKE %:pattern%")
    List<AvailableNetwork> findBySsidContaining(@Param("pattern") String pattern);

    /**
     * Find networks with SSID starting with pattern
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.ssid LIKE :pattern%")
    List<AvailableNetwork> findBySsidStartingWith(@Param("pattern") String pattern);

    /**
     * Find hidden networks (empty or null SSID)
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE (an.ssid IS NULL OR an.ssid = '' OR an.ssid = '<hidden>')")
    List<AvailableNetwork> findHiddenNetworks();

    // ==========================================
    // CONNECTION ANALYSIS QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find most connected networks
     */
    @Query("SELECT an, COUNT(nc) as connectionCount FROM AvailableNetwork an LEFT JOIN NetworkConnection nc ON nc.network = an GROUP BY an ORDER BY connectionCount DESC")
    List<Object[]> findMostConnectedNetworks();

    /**
     * Find networks never connected to
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.id NOT IN (SELECT DISTINCT nc.network.id FROM NetworkConnection nc WHERE nc.network IS NOT NULL)")
    List<AvailableNetwork> findNeverConnectedNetworks();

    // ==========================================
    // QUALITY AND PERFORMANCE QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find high-quality networks (strong signal + secure)
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.signalStrength >= :minSignal AND an.isSecured = true ORDER BY an.signalStrength DESC")
    List<AvailableNetwork> findHighQualityNetworks(@Param("minSignal") Integer minSignal);

    /**
     * Find networks with poor signal quality
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.signalStrength < :threshold AND an.isAvailable = true")
    List<AvailableNetwork> findPoorSignalNetworks(@Param("threshold") Integer threshold);

    /**
     * Find best available networks (available, secure, strong signal)
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.isAvailable = true AND an.isSecured = true AND an.signalStrength >= :minSignal ORDER BY an.signalStrength DESC")
    List<AvailableNetwork> findBestAvailableNetworks(@Param("minSignal") Integer minSignal);

    // ==========================================
    // MAINTENANCE AND CLEANUP QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find old network records for cleanup
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.lastSeen < :cutoff AND an.isAvailable = false")
    List<AvailableNetwork> findOldNetworkRecords(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Count networks by availability status
     */
    @Query("SELECT an.isAvailable, COUNT(an) FROM AvailableNetwork an GROUP BY an.isAvailable")
    List<Object[]> countByAvailability();

    /**
     * Find duplicate networks (same SSID, different BSSID)
     */
    @Query("SELECT an.ssid, COUNT(an) FROM AvailableNetwork an GROUP BY an.ssid HAVING COUNT(an) > 1")
    List<Object[]> findDuplicateSSIDs();

    // ==========================================
    // REAL-TIME MONITORING QUERIES - ACTIVELY USED
    // ==========================================

    /**
     * Find networks that appeared recently (new discoveries)
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.firstDetected >= :since ORDER BY an.firstDetected DESC")
    List<AvailableNetwork> findNewlyDiscoveredNetworks(@Param("since") LocalDateTime since);

    /**
     * Find networks that disappeared recently
     */
    @Query("SELECT an FROM AvailableNetwork an WHERE an.isAvailable = false AND an.lastSeen >= :since AND an.lastSeen < :cutoff")
    List<AvailableNetwork> findRecentlyDisappearedNetworks(@Param("since") LocalDateTime since, @Param("cutoff") LocalDateTime cutoff);

    /**
     * Get network environment summary
     */
    @Query("SELECT " +
            "COUNT(an) as totalNetworks, " +
            "COUNT(CASE WHEN an.isAvailable = true THEN 1 END) as availableNetworks, " +
            "COUNT(CASE WHEN an.isConnected = true THEN 1 END) as connectedNetworks, " +
            "COUNT(CASE WHEN an.isSecured = true THEN 1 END) as securedNetworks, " +
            "AVG(an.signalStrength) as averageSignal, " +
            "MAX(an.signalStrength) as maxSignal " +
            "FROM AvailableNetwork an")
    Object[] getNetworkEnvironmentSummary();
}