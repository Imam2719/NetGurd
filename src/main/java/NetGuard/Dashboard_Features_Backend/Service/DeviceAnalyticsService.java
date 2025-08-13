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

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceAnalyticsService {

    private final NetworkConnectionRepository connectionRepository;
    private final AvailableNetworkRepository networkRepository;

    /**
     * Get device analytics for specified time range
     */
    @Transactional(readOnly = true)
    public List<DeviceAnalyticsDTO> getDeviceAnalytics(String timeRange) {
        LocalDateTime startTime = calculateStartTime(timeRange);

        List<NetworkConnection> connections = connectionRepository.findConnectionsSince(startTime);

        return connections.stream()
                .collect(Collectors.groupingBy(NetworkConnection::getDeviceMac))
                .entrySet()
                .stream()
                .map(entry -> {
                    String deviceMac = entry.getKey();
                    List<NetworkConnection> deviceConnections = entry.getValue();

                    // Get most recent connection for device info
                    NetworkConnection latestConnection = deviceConnections.stream()
                            .max(Comparator.comparing(NetworkConnection::getConnectedAt))
                            .orElse(deviceConnections.get(0));

                    // Calculate aggregated data
                    Long totalDataUsage = deviceConnections.stream()
                            .mapToLong(conn -> conn.getDataUsageBytes() != null ? conn.getDataUsageBytes() : 0L)
                            .sum();

                    Integer totalDuration = deviceConnections.stream()
                            .mapToInt(conn -> conn.getConnectionDurationMinutes() != null ? conn.getConnectionDurationMinutes() : 0)
                            .sum();

                    // Get current browsing activity
                    List<String> frequentSites = getCurrentBrowsingForDevice(deviceMac);

                    return new DeviceAnalyticsDTO(
                            latestConnection.getDeviceName(),
                            deviceMac,
                            determineDeviceType(latestConnection.getDeviceName()),
                            totalDataUsage,
                            totalDuration,
                            latestConnection.getNetwork() != null ?
                                    latestConnection.getNetwork().getSignalStrength() : 0,
                            latestConnection.getConnectionStatus(),
                            latestConnection.getConnectedAt(),
                            frequentSites,
                            false // TODO: Implement device blocking logic
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Get current real-time device activity
     */
    @Transactional(readOnly = true)
    public List<DeviceActivityDTO> getCurrentDeviceActivity() {
        List<NetworkConnection> activeConnections = connectionRepository.findByIsCurrentlyConnectedTrue();

        return activeConnections.stream()
                .map(connection -> {
                    String currentSite = getCurrentBrowsingActivity(connection.getDeviceMac());
                    String currentActivity = determineCurrentActivity(connection.getDeviceName(), currentSite);
                    Integer batteryLevel = getBatteryLevel(connection.getDeviceMac());

                    return new DeviceActivityDTO(
                            connection.getDeviceName(),
                            connection.getDeviceMac(),
                            connection.getAssignedIp(),
                            currentSite,
                            currentActivity,
                            connection.getNetwork() != null ?
                                    connection.getNetwork().getSignalStrength() : 0,
                            connection.getDataUsageBytes() != null ? connection.getDataUsageBytes() : 0L,
                            LocalDateTime.now(),
                            true,
                            determineDeviceType(connection.getDeviceName()),
                            batteryLevel
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Get network usage statistics
     */
    @Transactional(readOnly = true)
    public NetworkUsageStatsDTO getNetworkUsage(String period) {
        LocalDateTime startTime = calculateStartTime(period);
        List<NetworkConnection> connections = connectionRepository.findConnectionsSince(startTime);

        // Calculate total usage
        Long totalBytes = connections.stream()
                .mapToLong(conn -> conn.getDataUsageBytes() != null ? conn.getDataUsageBytes() : 0L)
                .sum();

        // Simulate upload/download split (typically 20% upload, 80% download)
        Long uploadBytes = (long) (totalBytes * 0.2);
        Long downloadBytes = totalBytes - uploadBytes;

        // Get peak device count
        Integer peakDevices = connectionRepository.findByIsCurrentlyConnectedTrue().size();

        // Calculate average signal
        Double avgSignal = networkRepository.getAverageSignalStrength();

        // Generate hourly breakdown
        List<HourlyUsageDTO> hourlyBreakdown = generateHourlyBreakdown(connections);

        // Generate device breakdown
        List<DeviceUsageDTO> deviceBreakdown = generateDeviceBreakdown(connections, totalBytes);

        return new NetworkUsageStatsDTO(
                totalBytes,
                uploadBytes,
                downloadBytes,
                peakDevices,
                avgSignal != null ? avgSignal : 0.0,
                hourlyBreakdown,
                deviceBreakdown,
                period,
                LocalDateTime.now()
        );
    }

    /**
     * Get current browsing data for all devices
     */
    @Transactional(readOnly = true)
    public List<BrowsingDataDTO> getCurrentBrowsingActivity() {
        List<NetworkConnection> activeConnections = connectionRepository.findByIsCurrentlyConnectedTrue();

        return activeConnections.stream()
                .map(connection -> {
                    String currentSite = getCurrentBrowsingActivity(connection.getDeviceMac());
                    String category = categorizeSite(currentSite);
                    String browserType = detectBrowserType(connection.getDeviceName());

                    return new BrowsingDataDTO(
                            connection.getDeviceName(),
                            connection.getDeviceMac(),
                            currentSite,
                            category,
                            LocalDateTime.now(),
                            calculateBrowsingTime(connection.getConnectedAt()),
                            isSecureSite(currentSite),
                            false, // TODO: Implement VPN detection
                            browserType,
                            getActiveTabsCount(connection.getDeviceMac())
                    );
                })
                .filter(browsing -> browsing.getCurrentSite() != null)
                .collect(Collectors.toList());
    }

    /**
     * Get comprehensive dashboard analytics
     */
    @Transactional(readOnly = true)
    public DashboardAnalyticsDTO getDashboardAnalytics() {
        List<DeviceAnalyticsDTO> deviceAnalytics = getDeviceAnalytics("24h");
        List<DeviceActivityDTO> currentActivity = getCurrentDeviceActivity();
        List<BrowsingDataDTO> browsingData = getCurrentBrowsingActivity();
        NetworkUsageStatsDTO networkUsage = getNetworkUsage("24h");

        // Generate dashboard summary
        DashboardSummaryDTO summary = new DashboardSummaryDTO(
                deviceAnalytics.size(),
                currentActivity.size(),
                0, // TODO: Implement blocked devices count
                networkUsage.getTotalBytesTransferred() / (1024L * 1024L * 1024L), // Convert to GB
                networkUsage.getAverageSignalStrength().intValue(),
                getMostUsedSite(browsingData),
                getPeakUsageHour(networkUsage.getHourlyBreakdown()),
                checkAllDevicesSecure(currentActivity)
        );

        return new DashboardAnalyticsDTO(
                deviceAnalytics,
                currentActivity,
                browsingData,
                networkUsage,
                summary,
                LocalDateTime.now()
        );
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private LocalDateTime calculateStartTime(String timeRange) {
        LocalDateTime now = LocalDateTime.now();

        switch (timeRange.toLowerCase()) {
            case "1h":
                return now.minusHours(1);
            case "6h":
                return now.minusHours(6);
            case "24h":
            case "1d":
                return now.minusDays(1);
            case "3d":
                return now.minusDays(3);
            case "7d":
                return now.minusDays(7);
            case "15d":
                return now.minusDays(15);
            case "30d":
                return now.minusDays(30);
            default:
                return now.minusDays(1);
        }
    }

    private String determineDeviceType(String deviceName) {
        if (deviceName == null) return "unknown";

        String name = deviceName.toLowerCase();
        if (name.contains("iphone") || name.contains("android") || name.contains("phone")) {
            return "mobile";
        } else if (name.contains("ipad") || name.contains("tablet")) {
            return "tablet";
        } else if (name.contains("tv") || name.contains("smart tv")) {
            return "tv";
        } else if (name.contains("laptop") || name.contains("macbook")) {
            return "laptop";
        } else if (name.contains("desktop") || name.contains("pc")) {
            return "desktop";
        }
        return "unknown";
    }

    private List<String> getCurrentBrowsingForDevice(String deviceMac) {
        // This would integrate with network monitoring tools
        // For now, return sample data based on device type
        List<String> frequentSites = new ArrayList<>();

        if (deviceMac == null) {
            return Arrays.asList("google.com", "example.com");
        }

        // Simulate based on device MAC (last octet for variety)
        String lastOctet = deviceMac.substring(Math.max(0, deviceMac.lastIndexOf(":") + 1));
        int variety = 0;
        try {
            variety = Integer.parseInt(lastOctet, 16) % 5;
        } catch (NumberFormatException e) {
            variety = Math.abs(deviceMac.hashCode()) % 5;
        }

        switch (variety) {
            case 0:
                frequentSites.addAll(Arrays.asList("youtube.com", "netflix.com", "spotify.com"));
                break;
            case 1:
                frequentSites.addAll(Arrays.asList("github.com", "stackoverflow.com", "google.com"));
                break;
            case 2:
                frequentSites.addAll(Arrays.asList("facebook.com", "instagram.com", "twitter.com"));
                break;
            case 3:
                frequentSites.addAll(Arrays.asList("amazon.com", "ebay.com", "shopping.com"));
                break;
            default:
                frequentSites.addAll(Arrays.asList("news.com", "wikipedia.org", "reddit.com"));
        }

        return frequentSites;
    }

    private String getCurrentBrowsingActivity(String deviceMac) {
        List<String> frequentSites = getCurrentBrowsingForDevice(deviceMac);

        if (!frequentSites.isEmpty()) {
            return frequentSites.get(new Random(deviceMac.hashCode()).nextInt(frequentSites.size()));
        }

        return null;
    }

    private String determineCurrentActivity(String deviceName, String currentSite) {
        if (currentSite == null) return "Idle";

        if (currentSite.contains("youtube") || currentSite.contains("netflix")) {
            return "Streaming Video";
        } else if (currentSite.contains("spotify") || currentSite.contains("music")) {
            return "Streaming Music";
        } else if (currentSite.contains("github") || currentSite.contains("stackoverflow")) {
            return "Development Work";
        } else if (currentSite.contains("facebook") || currentSite.contains("instagram") || currentSite.contains("twitter")) {
            return "Social Media";
        } else if (currentSite.contains("amazon") || currentSite.contains("shopping")) {
            return "Online Shopping";
        } else {
            return "Web Browsing";
        }
    }

    private Integer getBatteryLevel(String deviceMac) {
        String deviceType = determineDeviceTypeFromMac(deviceMac);

        if ("mobile".equals(deviceType) || "tablet".equals(deviceType)) {
            return 30 + new Random(deviceMac.hashCode()).nextInt(71);
        }

        return null; // Non-mobile devices
    }

    private String determineDeviceTypeFromMac(String deviceMac) {
        // This is a simplified implementation
        // In reality, you'd use OUI (Organizationally Unique Identifier) lookup
        return "mobile"; // Default assumption for battery-powered devices
    }

    private List<HourlyUsageDTO> generateHourlyBreakdown(List<NetworkConnection> connections) {
        Map<Integer, List<NetworkConnection>> hourlyConnections = connections.stream()
                .collect(Collectors.groupingBy(
                        conn -> conn.getConnectedAt().getHour()
                ));

        List<HourlyUsageDTO> hourlyBreakdown = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            List<NetworkConnection> hourConnections = hourlyConnections.getOrDefault(hour, new ArrayList<>());

            Long bytesUsed = hourConnections.stream()
                    .mapToLong(conn -> conn.getDataUsageBytes() != null ? conn.getDataUsageBytes() : 0L)
                    .sum();

            Integer deviceCount = hourConnections.size();

            Double averageSignal = hourConnections.stream()
                    .filter(conn -> conn.getNetwork() != null)
                    .mapToInt(conn -> conn.getNetwork().getSignalStrength())
                    .average()
                    .orElse(0.0);

            hourlyBreakdown.add(new HourlyUsageDTO(hour, bytesUsed, deviceCount, averageSignal));
        }

        return hourlyBreakdown;
    }

    private List<DeviceUsageDTO> generateDeviceBreakdown(List<NetworkConnection> connections, Long totalBytes) {
        return connections.stream()
                .collect(Collectors.groupingBy(NetworkConnection::getDeviceMac))
                .entrySet()
                .stream()
                .map(entry -> {
                    String deviceMac = entry.getKey();
                    List<NetworkConnection> deviceConnections = entry.getValue();

                    String deviceName = deviceConnections.get(0).getDeviceName();

                    Long deviceBytes = deviceConnections.stream()
                            .mapToLong(conn -> conn.getDataUsageBytes() != null ? conn.getDataUsageBytes() : 0L)
                            .sum();

                    Integer connectionMinutes = deviceConnections.stream()
                            .mapToInt(conn -> conn.getConnectionDurationMinutes() != null ? conn.getConnectionDurationMinutes() : 0)
                            .sum();

                    Double percentage = totalBytes > 0 ? (deviceBytes.doubleValue() / totalBytes * 100) : 0.0;

                    return new DeviceUsageDTO(deviceName, deviceMac, deviceBytes, connectionMinutes, percentage);
                })
                .sorted((a, b) -> b.getBytesUsed().compareTo(a.getBytesUsed()))
                .collect(Collectors.toList());
    }

    private String categorizeSite(String site) {
        if (site == null) return "Unknown";

        site = site.toLowerCase();

        if (site.contains("youtube") || site.contains("netflix") || site.contains("hulu")) {
            return "Entertainment";
        } else if (site.contains("facebook") || site.contains("instagram") || site.contains("twitter")) {
            return "Social Media";
        } else if (site.contains("github") || site.contains("stackoverflow") || site.contains("documentation")) {
            return "Development";
        } else if (site.contains("amazon") || site.contains("ebay") || site.contains("shop")) {
            return "Shopping";
        } else if (site.contains("news") || site.contains("cnn") || site.contains("bbc")) {
            return "News";
        } else if (site.contains("google") || site.contains("search")) {
            return "Search";
        } else {
            return "General";
        }
    }

    private String detectBrowserType(String deviceName) {
        if (deviceName == null) return "Chrome";

        String name = deviceName.toLowerCase();

        if (name.contains("iphone") || name.contains("ipad") || name.contains("mac")) {
            return "Safari";
        } else if (name.contains("android")) {
            return "Chrome Mobile";
        } else {
            return "Chrome";
        }
    }

    private Long calculateBrowsingTime(LocalDateTime connectedAt) {
        return java.time.Duration.between(connectedAt, LocalDateTime.now()).toMinutes();
    }

    private Boolean isSecureSite(String site) {
        if (site == null) return false;

        // Most modern sites use HTTPS
        return !site.startsWith("http://") &&
                (site.contains("google") || site.contains("github") || site.contains("amazon") ||
                        site.contains("facebook") || site.contains("netflix"));
    }

    private Integer getActiveTabsCount(String deviceMac) {
        // This would integrate with browser APIs
        // Simulate based on device activity
        return 3 + new Random(deviceMac.hashCode()).nextInt(8); // 3-10 tabs
    }

    private String getMostUsedSite(List<BrowsingDataDTO> browsingData) {
        return browsingData.stream()
                .collect(Collectors.groupingBy(BrowsingDataDTO::getCurrentSite))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue(Comparator.comparing(list -> list.size())))
                .map(Map.Entry::getKey)
                .orElse("No data");
    }

    private String getPeakUsageHour(List<HourlyUsageDTO> hourlyBreakdown) {
        return hourlyBreakdown.stream()
                .max(Comparator.comparing(HourlyUsageDTO::getBytesUsed))
                .map(hour -> String.format("%02d:00", hour.getHour()))
                .orElse("No data");
    }

    private Boolean checkAllDevicesSecure(List<DeviceActivityDTO> activities) {
        return activities.stream()
                .allMatch(activity ->
                        activity.getCurrentSite() == null ||
                                isSecureSite(activity.getCurrentSite())
                );
    }
}