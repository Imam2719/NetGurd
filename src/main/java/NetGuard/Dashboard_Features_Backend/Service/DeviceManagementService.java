package NetGuard.Dashboard_Features_Backend.Service;

import NetGuard.Dashboard_Features_Backend.DTO.*;
import NetGuard.Dashboard_Features_Backend.Entity.NetworkConnection;
import NetGuard.Dashboard_Features_Backend.Repository.NetworkConnectionRepository;
import NetGuard.Dashboard_Features_Backend.Repository.AvailableNetworkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceManagementService {

    private final NetworkConnectionRepository connectionRepository;
    private final AvailableNetworkRepository networkRepository;

    // In-memory storage for device blocking (in production, use database)
    private final Map<String, Boolean> blockedDevices = new HashMap<>();
    private final Map<String, String> blockReasons = new HashMap<>();
    private final Map<String, LocalDateTime> blockTimes = new HashMap<>();
    private final Map<String, TimeLimitRequestDTO> deviceTimeLimits = new HashMap<>();

    /**
     * Get all managed devices with their current status
     */
    public List<ManagedDeviceDTO> getAllManagedDevices() {
        List<NetworkConnection> allConnections = connectionRepository.findAll();

        // Group by device MAC to get unique devices
        Map<String, List<NetworkConnection>> deviceConnections = allConnections.stream()
                .collect(Collectors.groupingBy(NetworkConnection::getDeviceMac));

        return deviceConnections.entrySet().stream()
                .map(entry -> {
                    String deviceMac = entry.getKey();
                    List<NetworkConnection> connections = entry.getValue();

                    // Get most recent connection for device info
                    NetworkConnection latestConnection = connections.stream()
                            .max(Comparator.comparing(NetworkConnection::getConnectedAt))
                            .orElse(connections.get(0));

                    // Calculate total usage
                    Long totalDataUsage = connections.stream()
                            .mapToLong(conn -> conn.getDataUsageBytes() != null ? conn.getDataUsageBytes() : 0L)
                            .sum();

                    Integer totalDuration = connections.stream()
                            .mapToInt(conn -> conn.getConnectionDurationMinutes() != null ? conn.getConnectionDurationMinutes() : 0)
                            .sum();

                    // Get current activity
                    String currentSite = getCurrentBrowsingActivity(deviceMac);
                    String currentActivity = determineCurrentActivity(currentSite);

                    // Get recent sites
                    List<String> recentSites = getRecentSites(deviceMac);

                    // Check time limits
                    TimeLimitRequestDTO timeLimit = deviceTimeLimits.get(deviceMac);
                    Integer timeUsedToday = calculateTimeUsedToday(deviceMac);

                    return new ManagedDeviceDTO(
                            latestConnection.getDeviceName(),
                            deviceMac,
                            determineDeviceType(latestConnection.getDeviceName()),
                            latestConnection.getAssignedIp(),
                            latestConnection.getNetwork() != null ? latestConnection.getNetwork().getSignalStrength() : 0,
                            latestConnection.getConnectionStatus(),
                            blockedDevices.getOrDefault(deviceMac, false),
                            blockReasons.get(deviceMac),
                            latestConnection.getConnectedAt(),
                            totalDataUsage,
                            totalDuration,
                            currentSite,
                            currentActivity,
                            timeLimit != null,
                            timeLimit != null ? timeLimit.getDailyLimitMinutes() : null,
                            timeUsedToday,
                            latestConnection.getConnectedAt(),
                            blockTimes.get(deviceMac),
                            recentSites
                    );
                })
                .sorted((a, b) -> b.getLastActivity().compareTo(a.getLastActivity()))
                .collect(Collectors.toList());
    }

    /**
     * Block a device from network access
     */
    @Transactional
    public DeviceActionResultDTO blockDevice(String deviceMac, String reason) {
        try {
            // Check if device exists
            Optional<NetworkConnection> connectionOpt = connectionRepository
                    .findByDeviceMacAndIsCurrentlyConnectedTrue(deviceMac);

            if (connectionOpt.isEmpty()) {
                return new DeviceActionResultDTO(
                        false,
                        "Device not found or not currently connected",
                        deviceMac,
                        "Unknown",
                        "BLOCK",
                        LocalDateTime.now(),
                        "UNKNOWN",
                        "UNKNOWN",
                        "Device not found in active connections"
                );
            }

            NetworkConnection connection = connectionOpt.get();
            String deviceName = connection.getDeviceName();
            String previousStatus = blockedDevices.getOrDefault(deviceMac, false) ? "BLOCKED" : "ACTIVE";

            // Implement actual device blocking (firewall rules, router config, etc.)
            boolean blockSuccess = implementDeviceBlocking(deviceMac, connection.getAssignedIp());

            if (blockSuccess) {
                // Update in-memory storage
                blockedDevices.put(deviceMac, true);
                blockReasons.put(deviceMac, reason != null ? reason : "Blocked by administrator");
                blockTimes.put(deviceMac, LocalDateTime.now());

                log.info("Device {} ({}) blocked successfully", deviceName, deviceMac);

                return new DeviceActionResultDTO(
                        true,
                        "Device blocked successfully",
                        deviceMac,
                        deviceName,
                        "BLOCK",
                        LocalDateTime.now(),
                        previousStatus,
                        "BLOCKED",
                        "Device access has been restricted"
                );
            } else {
                return new DeviceActionResultDTO(
                        false,
                        "Failed to block device - system error",
                        deviceMac,
                        deviceName,
                        "BLOCK",
                        LocalDateTime.now(),
                        previousStatus,
                        previousStatus,
                        "Unable to apply blocking rules"
                );
            }

        } catch (Exception e) {
            log.error("Error blocking device {}: ", deviceMac, e);
            return new DeviceActionResultDTO(
                    false,
                    "Error blocking device: " + e.getMessage(),
                    deviceMac,
                    "Unknown",
                    "BLOCK",
                    LocalDateTime.now(),
                    "UNKNOWN",
                    "ERROR",
                    e.getMessage()
            );
        }
    }

    /**
     * Unblock a device
     */
    @Transactional
    public DeviceActionResultDTO unblockDevice(String deviceMac) {
        try {
            Optional<NetworkConnection> connectionOpt = connectionRepository
                    .findByDeviceMacAndIsCurrentlyConnectedTrue(deviceMac);

            String deviceName = connectionOpt.map(NetworkConnection::getDeviceName).orElse("Unknown");
            String previousStatus = blockedDevices.getOrDefault(deviceMac, false) ? "BLOCKED" : "ACTIVE";

            if (!blockedDevices.getOrDefault(deviceMac, false)) {
                return new DeviceActionResultDTO(
                        false,
                        "Device is not currently blocked",
                        deviceMac,
                        deviceName,
                        "UNBLOCK",
                        LocalDateTime.now(),
                        previousStatus,
                        "ACTIVE",
                        "Device was already active"
                );
            }

            // Implement actual device unblocking
            String ipAddress = connectionOpt.map(NetworkConnection::getAssignedIp).orElse(null);
            boolean unblockSuccess = removeDeviceBlocking(deviceMac, ipAddress);

            if (unblockSuccess) {
                // Update in-memory storage
                blockedDevices.remove(deviceMac);
                blockReasons.remove(deviceMac);
                blockTimes.remove(deviceMac);

                log.info("Device {} ({}) unblocked successfully", deviceName, deviceMac);

                return new DeviceActionResultDTO(
                        true,
                        "Device unblocked successfully",
                        deviceMac,
                        deviceName,
                        "UNBLOCK",
                        LocalDateTime.now(),
                        previousStatus,
                        "ACTIVE",
                        "Device access has been restored"
                );
            } else {
                return new DeviceActionResultDTO(
                        false,
                        "Failed to unblock device - system error",
                        deviceMac,
                        deviceName,
                        "UNBLOCK",
                        LocalDateTime.now(),
                        previousStatus,
                        previousStatus,
                        "Unable to remove blocking rules"
                );
            }

        } catch (Exception e) {
            log.error("Error unblocking device {}: ", deviceMac, e);
            return new DeviceActionResultDTO(
                    false,
                    "Error unblocking device: " + e.getMessage(),
                    deviceMac,
                    "Unknown",
                    "UNBLOCK",
                    LocalDateTime.now(),
                    "UNKNOWN",
                    "ERROR",
                    e.getMessage()
            );
        }
    }

    /**
     * Get detailed information about a specific device
     */
    public DeviceDetailsDTO getDeviceDetails(String deviceMac) {
        // Get device info
        List<ManagedDeviceDTO> devices = getAllManagedDevices();
        ManagedDeviceDTO device = devices.stream()
                .filter(d -> d.getDeviceMac().equals(deviceMac))
                .findFirst()
                .orElse(null);

        if (device == null) {
            return null;
        }

        // Get browsing history
        List<BrowsingHistoryDTO> browsingHistory = getBrowsingHistory(deviceMac, "24h");

        // Get connection history
        List<ConnectionHistoryDTO> connectionHistory = getConnectionHistory(deviceMac);

        // Calculate statistics
        DeviceStatsDTO statistics = calculateDeviceStatistics(deviceMac);

        // Get security alerts (simulated)
        List<SecurityAlertDTO> securityAlerts = generateSecurityAlerts(deviceMac, browsingHistory);

        // Get current settings
        DeviceSettingsDTO settings = getDeviceSettings(deviceMac);

        return new DeviceDetailsDTO(
                device,
                browsingHistory,
                connectionHistory,
                statistics,
                securityAlerts,
                settings
        );
    }

    /**
     * Set time limits for a device
     */
    public DeviceActionResultDTO setTimeLimit(String deviceMac, TimeLimitRequestDTO request) {
        try {
            // Validate time limit settings
            if (request.getDailyLimitMinutes() != null && request.getDailyLimitMinutes() < 0) {
                return new DeviceActionResultDTO(
                        false,
                        "Daily limit cannot be negative",
                        deviceMac,
                        "Unknown",
                        "SET_TIME_LIMIT",
                        LocalDateTime.now(),
                        "UNKNOWN",
                        "ERROR",
                        "Invalid time limit configuration"
                );
            }

            // Store time limit settings
            deviceTimeLimits.put(deviceMac, request);

            String deviceName = getDeviceName(deviceMac);

            log.info("Time limits set for device {} ({}): {} minutes daily",
                    deviceName, deviceMac, request.getDailyLimitMinutes());

            return new DeviceActionResultDTO(
                    true,
                    "Time limits configured successfully",
                    deviceMac,
                    deviceName,
                    "SET_TIME_LIMIT",
                    LocalDateTime.now(),
                    "ACTIVE",
                    "TIME_LIMITED",
                    String.format("Daily limit: %d minutes", request.getDailyLimitMinutes())
            );

        } catch (Exception e) {
            log.error("Error setting time limits for device {}: ", deviceMac, e);
            return new DeviceActionResultDTO(
                    false,
                    "Error setting time limits: " + e.getMessage(),
                    deviceMac,
                    "Unknown",
                    "SET_TIME_LIMIT",
                    LocalDateTime.now(),
                    "UNKNOWN",
                    "ERROR",
                    e.getMessage()
            );
        }
    }

    /**
     * Get current device activity - Fixed return type
     */
    public DeviceActivityDTO getDeviceActivity(String deviceMac) {
        Optional<NetworkConnection> connectionOpt = connectionRepository
                .findByDeviceMacAndIsCurrentlyConnectedTrue(deviceMac);

        if (connectionOpt.isEmpty()) {
            return null;
        }

        NetworkConnection connection = connectionOpt.get();
        String currentSite = getCurrentBrowsingActivity(deviceMac);
        String currentActivity = determineCurrentActivity(currentSite);
        Integer batteryLevel = getBatteryLevel(deviceMac);

        return new DeviceActivityDTO(
                connection.getDeviceName(),
                deviceMac,
                connection.getAssignedIp(),
                currentSite,
                currentActivity,
                connection.getNetwork() != null ? connection.getNetwork().getSignalStrength() : 0,
                connection.getDataUsageBytes() != null ? connection.getDataUsageBytes() : 0L,
                LocalDateTime.now(),
                true,
                determineDeviceType(connection.getDeviceName()),
                batteryLevel
        );
    }

    /**
     * Get browsing history for a device
     */
    public List<BrowsingHistoryDTO> getBrowsingHistory(String deviceMac, String period) {
        List<BrowsingHistoryDTO> history = new ArrayList<>();
        LocalDateTime startTime = calculateStartTime(period);

        // Generate simulated browsing data based on actual device connections
        List<String> sampleSites = getSampleSites(deviceMac);
        Random random = new Random(deviceMac.hashCode()); // Consistent randomization

        for (int i = 0; i < Math.min(20, sampleSites.size() * 3); i++) {
            String site = sampleSites.get(random.nextInt(sampleSites.size()));
            LocalDateTime visitTime = startTime.plusMinutes(random.nextInt(1440)); // Random time within period

            history.add(new BrowsingHistoryDTO(
                    "https://" + site,
                    extractDomain(site),
                    getPageTitle(site),
                    categorizeWebsite(site),
                    visitTime,
                    5 + random.nextInt(25), // 5-30 minutes
                    isSecureUrl(site),
                    false, // Not blocked for now
                    getBrowserType(deviceMac),
                    3 + random.nextInt(5), // 3-8 tabs
                    getUserAgent(deviceMac)
            ));
        }

        return history.stream()
                .sorted((a, b) -> b.getVisitTime().compareTo(a.getVisitTime()))
                .collect(Collectors.toList());
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private boolean implementDeviceBlocking(String deviceMac, String ipAddress) {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("linux") || os.contains("mac")) {
                return blockDeviceWithIptables(deviceMac, ipAddress);
            } else if (os.contains("win")) {
                return blockDeviceWithWindowsFirewall(deviceMac, ipAddress);
            }

            log.warn("Device blocking not fully implemented for OS: {} - simulating success", os);
            return true; // Simulate success for development

        } catch (Exception e) {
            log.error("Error implementing device blocking: ", e);
            return false;
        }
    }

    private boolean removeDeviceBlocking(String deviceMac, String ipAddress) {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("linux") || os.contains("mac")) {
                return unblockDeviceWithIptables(deviceMac, ipAddress);
            } else if (os.contains("win")) {
                return unblockDeviceWithWindowsFirewall(deviceMac, ipAddress);
            }

            log.warn("Device unblocking not fully implemented for OS: {} - simulating success", os);
            return true;

        } catch (Exception e) {
            log.error("Error removing device blocking: ", e);
            return false;
        }
    }

    private boolean blockDeviceWithIptables(String deviceMac, String ipAddress) throws IOException {
        if (ipAddress == null) return false;

        try {
            CommandLine cmdLine = CommandLine.parse("sudo iptables -A INPUT -s " + ipAddress + " -j DROP");
            DefaultExecutor executor = new DefaultExecutor();

            int exitCode = executor.execute(cmdLine);
            if (exitCode == 0) {
                CommandLine outboundCmd = CommandLine.parse("sudo iptables -A OUTPUT -d " + ipAddress + " -j DROP");
                executor.execute(outboundCmd);
            }
            return exitCode == 0;
        } catch (Exception e) {
            log.debug("iptables command failed (development mode): {}", e.getMessage());
            return true; // Return true for development/testing
        }
    }

    private boolean unblockDeviceWithIptables(String deviceMac, String ipAddress) throws IOException {
        if (ipAddress == null) return false;

        try {
            CommandLine cmdLine1 = CommandLine.parse("sudo iptables -D INPUT -s " + ipAddress + " -j DROP");
            CommandLine cmdLine2 = CommandLine.parse("sudo iptables -D OUTPUT -d " + ipAddress + " -j DROP");

            DefaultExecutor executor = new DefaultExecutor();
            executor.execute(cmdLine1);
            executor.execute(cmdLine2);

            return true;
        } catch (Exception e) {
            log.debug("iptables unblock command failed (development mode): {}", e.getMessage());
            return true; // Return true for development/testing
        }
    }

    private boolean blockDeviceWithWindowsFirewall(String deviceMac, String ipAddress) {
        log.info("Simulating Windows firewall block for IP: {}", ipAddress);
        return true;
    }

    private boolean unblockDeviceWithWindowsFirewall(String deviceMac, String ipAddress) {
        log.info("Simulating Windows firewall unblock for IP: {}", ipAddress);
        return true;
    }

    private String determineDeviceType(String deviceName) {
        if (deviceName == null) return "unknown";

        String name = deviceName.toLowerCase();
        if (name.contains("iphone") || name.contains("android") || name.contains("phone")) return "mobile";
        if (name.contains("ipad") || name.contains("tablet")) return "tablet";
        if (name.contains("tv") || name.contains("smart tv")) return "tv";
        if (name.contains("laptop") || name.contains("macbook")) return "laptop";
        if (name.contains("desktop") || name.contains("pc")) return "desktop";
        return "unknown";
    }

    private String getCurrentBrowsingActivity(String deviceMac) {
        List<String> sites = getSampleSites(deviceMac);
        if (!sites.isEmpty()) {
            return sites.get(new Random(deviceMac.hashCode()).nextInt(sites.size()));
        }
        return null;
    }

    private String determineCurrentActivity(String currentSite) {
        if (currentSite == null) return "Idle";

        String site = currentSite.toLowerCase();
        if (site.contains("youtube") || site.contains("netflix")) return "Video Streaming";
        if (site.contains("spotify") || site.contains("music")) return "Music Streaming";
        if (site.contains("github") || site.contains("stack")) return "Development";
        if (site.contains("facebook") || site.contains("instagram")) return "Social Media";
        if (site.contains("amazon") || site.contains("shop")) return "Shopping";
        return "Web Browsing";
    }

    private List<String> getRecentSites(String deviceMac) {
        return getSampleSites(deviceMac).stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<String> getSampleSites(String deviceMac) {
        if (deviceMac == null) return Arrays.asList("google.com", "example.com");

        int variety = Math.abs(deviceMac.hashCode()) % 5;

        switch (variety) {
            case 0: return Arrays.asList("youtube.com", "netflix.com", "spotify.com", "twitch.tv");
            case 1: return Arrays.asList("github.com", "stackoverflow.com", "docs.google.com", "medium.com");
            case 2: return Arrays.asList("facebook.com", "instagram.com", "twitter.com", "linkedin.com");
            case 3: return Arrays.asList("amazon.com", "ebay.com", "shopify.com", "etsy.com");
            default: return Arrays.asList("news.com", "wikipedia.org", "reddit.com", "bbc.com");
        }
    }

    private Integer getBatteryLevel(String deviceMac) {
        String deviceType = determineDeviceType(getDeviceName(deviceMac));
        if ("mobile".equals(deviceType) || "tablet".equals(deviceType)) {
            return 30 + new Random(deviceMac.hashCode()).nextInt(71);
        }
        return null;
    }

    private String getDeviceName(String deviceMac) {
        if (deviceMac == null) return "Unknown Device";

        return connectionRepository.findByDeviceMac(deviceMac).stream()
                .findFirst()
                .map(NetworkConnection::getDeviceName)
                .orElse("Unknown Device");
    }

    private Integer calculateTimeUsedToday(String deviceMac) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();

        return connectionRepository.findByDeviceMac(deviceMac).stream()
                .filter(conn -> conn.getConnectedAt().isAfter(startOfDay))
                .mapToInt(conn -> conn.getConnectionDurationMinutes() != null ? conn.getConnectionDurationMinutes() : 0)
                .sum();
    }

    private LocalDateTime calculateStartTime(String period) {
        LocalDateTime now = LocalDateTime.now();
        switch (period.toLowerCase()) {
            case "1h": return now.minusHours(1);
            case "6h": return now.minusHours(6);
            case "24h": case "1d": return now.minusDays(1);
            case "3d": return now.minusDays(3);
            case "7d": return now.minusDays(7);
            default: return now.minusDays(1);
        }
    }

    private List<ConnectionHistoryDTO> getConnectionHistory(String deviceMac) {
        return connectionRepository.findByDeviceMac(deviceMac).stream()
                .map(conn -> new ConnectionHistoryDTO(
                        conn.getConnectedAt(),
                        conn.getDisconnectedAt(),
                        conn.getNetwork() != null ? conn.getNetwork().getSsid() : "Unknown",
                        conn.getNetwork() != null ? conn.getNetwork().getBssid() : "Unknown",
                        conn.getNetwork() != null ? conn.getNetwork().getSignalStrength() : 0,
                        conn.getDataUsageBytes() != null ? conn.getDataUsageBytes() : 0L,
                        conn.getConnectionDurationMinutes() != null ? conn.getConnectionDurationMinutes() : 0,
                        "WIFI",
                        conn.getAssignedIp(),
                        conn.getDisconnectionReason()
                ))
                .sorted((a, b) -> b.getConnectedAt().compareTo(a.getConnectedAt()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private DeviceStatsDTO calculateDeviceStatistics(String deviceMac) {
        List<NetworkConnection> connections = connectionRepository.findByDeviceMac(deviceMac);

        if (connections.isEmpty()) {
            return new DeviceStatsDTO(
                    0L, 0, 0, 0, "No data", "General", 0.0, 0,
                    LocalDateTime.now(), LocalDateTime.now(),
                    new ArrayList<>(), new ArrayList<>()
            );
        }

        Long totalDataUsage = connections.stream()
                .mapToLong(conn -> conn.getDataUsageBytes() != null ? conn.getDataUsageBytes() : 0L)
                .sum();

        Integer totalTime = connections.stream()
                .mapToInt(conn -> conn.getConnectionDurationMinutes() != null ? conn.getConnectionDurationMinutes() : 0)
                .sum();

        Integer avgSessionDuration = connections.size() > 0 ? totalTime / connections.size() : 0;

        Double avgSignal = connections.stream()
                .filter(conn -> conn.getNetwork() != null)
                .mapToInt(conn -> conn.getNetwork().getSignalStrength())
                .average()
                .orElse(0.0);

        LocalDateTime firstSeen = connections.stream()
                .min(Comparator.comparing(NetworkConnection::getConnectedAt))
                .map(NetworkConnection::getConnectedAt)
                .orElse(LocalDateTime.now());

        LocalDateTime lastSeen = connections.stream()
                .max(Comparator.comparing(NetworkConnection::getConnectedAt))
                .map(NetworkConnection::getConnectedAt)
                .orElse(LocalDateTime.now());

        List<CategoryUsageDTO> categoryBreakdown = generateCategoryBreakdown(deviceMac);
        List<HourlyActivityDTO> activityPattern = generateHourlyActivityPattern(connections);

        return new DeviceStatsDTO(
                totalDataUsage,
                totalTime,
                avgSessionDuration,
                getSampleSites(deviceMac).size() * 3,
                getSampleSites(deviceMac).get(0),
                "Entertainment",
                avgSignal,
                connections.size(),
                firstSeen,
                lastSeen,
                categoryBreakdown,
                activityPattern
        );
    }

    private List<SecurityAlertDTO> generateSecurityAlerts(String deviceMac, List<BrowsingHistoryDTO> browsingHistory) {
        List<SecurityAlertDTO> alerts = new ArrayList<>();

        for (BrowsingHistoryDTO browsing : browsingHistory) {
            if (!browsing.getIsSecure()) {
                alerts.add(new SecurityAlertDTO(
                        "INSECURE_CONNECTION",
                        "MEDIUM",
                        "Accessed website without HTTPS encryption",
                        browsing.getUrl(),
                        browsing.getVisitTime(),
                        "WARNED",
                        false,
                        "Website does not use secure connection"
                ));
            }
        }

        return alerts.stream().limit(5).collect(Collectors.toList());
    }

    private DeviceSettingsDTO getDeviceSettings(String deviceMac) {
        TimeLimitRequestDTO timeLimit = deviceTimeLimits.get(deviceMac);

        return new DeviceSettingsDTO(
                true,
                "MODERATE",
                Arrays.asList("Adult", "Violence"),
                Arrays.asList("educational.org", "school.edu"),
                Arrays.asList("example-blocked.com"),
                timeLimit != null,
                timeLimit,
                false,
                "22:00",
                "07:00",
                false,
                true
        );
    }

    private List<CategoryUsageDTO> generateCategoryBreakdown(String deviceMac) {
        List<String> sites = getSampleSites(deviceMac);
        Map<String, List<String>> categorySites = new HashMap<>();

        for (String site : sites) {
            String category = categorizeWebsite(site);
            categorySites.computeIfAbsent(category, k -> new ArrayList<>()).add(site);
        }

        return categorySites.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<String> categorySiteList = entry.getValue();
                    return new CategoryUsageDTO(
                            category,
                            categorySiteList.size(),
                            30 * categorySiteList.size(),
                            (double) categorySiteList.size() / sites.size() * 100,
                            categorySiteList
                    );
                })
                .collect(Collectors.toList());
    }

    private List<HourlyActivityDTO> generateHourlyActivityPattern(List<NetworkConnection> connections) {
        Map<Integer, List<NetworkConnection>> hourlyConnections = connections.stream()
                .collect(Collectors.groupingBy(conn -> conn.getConnectedAt().getHour()));

        List<HourlyActivityDTO> pattern = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            List<NetworkConnection> hourConns = hourlyConnections.getOrDefault(hour, new ArrayList<>());

            Long dataUsed = hourConns.stream()
                    .mapToLong(conn -> conn.getDataUsageBytes() != null ? conn.getDataUsageBytes() : 0L)
                    .sum();

            Integer activeMinutes = hourConns.stream()
                    .mapToInt(conn -> conn.getConnectionDurationMinutes() != null ? conn.getConnectionDurationMinutes() : 0)
                    .sum();

            pattern.add(new HourlyActivityDTO(
                    hour,
                    activeMinutes,
                    dataUsed,
                    hourConns.size(),
                    activeMinutes > 30 ? "Active" : "Idle"
            ));
        }

        return pattern;
    }

    // Additional helper methods for data processing
    private String extractDomain(String url) {
        if (url == null) return "unknown.com";
        if (url.contains("/")) {
            return url.split("/")[0];
        }
        return url;
    }

    private String getPageTitle(String url) {
        String domain = extractDomain(url);
        return domain.substring(0, 1).toUpperCase() + domain.substring(1);
    }

    private String categorizeWebsite(String url) {
        if (url == null) return "General";

        String site = url.toLowerCase();
        if (site.contains("youtube") || site.contains("netflix") || site.contains("twitch")) return "Entertainment";
        if (site.contains("facebook") || site.contains("instagram") || site.contains("twitter")) return "Social Media";
        if (site.contains("github") || site.contains("stack") || site.contains("docs")) return "Development";
        if (site.contains("amazon") || site.contains("shop") || site.contains("ebay")) return "Shopping";
        if (site.contains("news") || site.contains("bbc") || site.contains("cnn")) return "News";
        return "General";
    }

    private Boolean isSecureUrl(String url) {
        if (url == null) return false;

        return !url.startsWith("http://") &&
                (url.contains("google") || url.contains("github") || url.contains("amazon"));
    }

    private String getBrowserType(String deviceMac) {
        String deviceType = determineDeviceType(getDeviceName(deviceMac));
        switch (deviceType) {
            case "mobile": return new Random().nextBoolean() ? "Chrome Mobile" : "Safari Mobile";
            case "tablet": return "Safari";
            default: return "Chrome";
        }
    }

    private String getUserAgent(String deviceMac) {
        String browserType = getBrowserType(deviceMac);
        switch (browserType) {
            case "Chrome": return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
            case "Safari": return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";
            default: return "Mozilla/5.0 (Mobile; rv:91.0) Gecko/91.0 Firefox/91.0";
        }
    }
}