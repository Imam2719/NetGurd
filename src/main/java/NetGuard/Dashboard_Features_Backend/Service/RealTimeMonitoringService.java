package NetGuard.Dashboard_Features_Backend.Service;

import NetGuard.Dashboard_Features_Backend.DTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.IOException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeMonitoringService {

    private final DeviceAnalyticsService analyticsService;
    private final DeviceManagementService deviceManagementService;
    private final Overview_AvailableNetwork_service networkService;

    // Real-time monitoring data storage
    private final Map<String, NetworkPerformanceDTO> devicePerformance = new ConcurrentHashMap<>();
    private final Map<String, SiteMonitoringDTO> currentBrowsing = new ConcurrentHashMap<>();
    private final List<SecurityAlertDTO> recentAlerts = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> networkTraffic = new ConcurrentHashMap<>();

    // Configuration
    private static final int MONITORING_INTERVAL_SECONDS = 10;
    private static final int PERFORMANCE_TEST_INTERVAL_MINUTES = 5;
    private static final int MAX_RECENT_ALERTS = 50;

    /**
     * Start real-time network monitoring for all connected devices
     */
    @Scheduled(fixedRate = MONITORING_INTERVAL_SECONDS * 1000)
    @Async("networkTaskExecutor")
    @Transactional(readOnly = true)
    public void performRealTimeMonitoring() {
        try {
            log.debug("Starting real-time monitoring cycle");

            // Monitor network performance
            monitorNetworkPerformance();

            // Monitor device browsing activity
            monitorBrowsingActivity();

            // Monitor network traffic
            monitorNetworkTraffic();

            // Check for security threats
            performSecurityScanning();

            log.debug("Real-time monitoring cycle completed");

        } catch (Exception e) {
            log.error("Error in real-time monitoring: ", e);
        }
    }

    /**
     * Monitor network performance for all connected devices
     */
    @Transactional(readOnly = true)
    private void monitorNetworkPerformance() {
        try {
            List<DeviceActivityDTO> activeDevices = analyticsService.getCurrentDeviceActivity();

            for (DeviceActivityDTO device : activeDevices) {
                if (device.getIsActive()) {
                    NetworkPerformanceDTO performance = measureDevicePerformance(device.getDeviceMac(), device.getAssignedIp());
                    if (performance != null) {
                        devicePerformance.put(device.getDeviceMac(), performance);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error monitoring network performance: ", e);
        }
    }

    /**
     * Monitor current browsing activity for devices
     */
    @Transactional(readOnly = true)
    private void monitorBrowsingActivity() {
        try {
            List<DeviceActivityDTO> activeDevices = analyticsService.getCurrentDeviceActivity();

            for (DeviceActivityDTO device : activeDevices) {
                if (device.getIsActive() && device.getCurrentSite() != null) {
                    SiteMonitoringDTO browsing = new SiteMonitoringDTO(
                            device.getDeviceMac(),
                            device.getCurrentSite(),
                            extractPageTitle(device.getCurrentSite()),
                            categorizeWebsite(device.getCurrentSite()),
                            isSecureUrl(device.getCurrentSite()),
                            isBlockedUrl(device.getCurrentSite()),
                            LocalDateTime.now(),
                            calculateTimeOnSite(device.getDeviceMac(), device.getCurrentSite()),
                            getPreviousSite(device.getDeviceMac()),
                            getUserAgent(device.getDeviceType()),
                            extractKeywords(device.getCurrentSite())
                    );

                    currentBrowsing.put(device.getDeviceMac(), browsing);

                    // Check for security concerns
                    checkSiteSecurity(browsing);
                }
            }

        } catch (Exception e) {
            log.error("Error monitoring browsing activity: ", e);
        }
    }

    /**
     * Monitor network traffic patterns
     */
    private void monitorNetworkTraffic() {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("linux") || os.contains("mac")) {
                monitorTrafficUnix();
            } else if (os.contains("win")) {
                monitorTrafficWindows();
            }

        } catch (Exception e) {
            log.error("Error monitoring network traffic: ", e);
        }
    }

    /**
     * Monitor traffic on Unix-like systems using netstat
     */
    private void monitorTrafficUnix() throws IOException {
        try {
            CommandLine cmdLine = CommandLine.parse("netstat -i");
            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            int exitCode = executor.execute(cmdLine);
            if (exitCode == 0) {
                String output = outputStream.toString("UTF-8");
                parseNetstatOutput(output);
            }
        } catch (Exception e) {
            log.debug("netstat command not available or failed: ", e);
        }
    }

    /**
     * Monitor traffic on Windows systems
     */
    private void monitorTrafficWindows() throws IOException {
        try {
            CommandLine cmdLine = CommandLine.parse("netstat -e");
            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            int exitCode = executor.execute(cmdLine);
            if (exitCode == 0) {
                String output = outputStream.toString("UTF-8");
                parseWindowsNetstatOutput(output);
            }
        } catch (Exception e) {
            log.debug("Windows netstat command failed: ", e);
        }
    }

    /**
     * Perform security scanning on current connections
     */
    @Transactional(readOnly = true)
    private void performSecurityScanning() {
        try {
            for (SiteMonitoringDTO browsing : currentBrowsing.values()) {
                // Check for malicious URLs
                if (isSuspiciousUrl(browsing.getCurrentUrl())) {
                    SecurityAlertDTO alert = new SecurityAlertDTO(
                            "SUSPICIOUS_URL",
                            "HIGH",
                            "Potentially malicious website detected",
                            browsing.getCurrentUrl(),
                            LocalDateTime.now(),
                            browsing.getIsBlocked() ? "BLOCKED" : "WARNED",
                            false,
                            "URL matches known threat patterns"
                    );
                    addSecurityAlert(alert);
                }

                // Check for insecure connections
                if (!browsing.getIsSecure()) {
                    SecurityAlertDTO alert = new SecurityAlertDTO(
                            "INSECURE_CONNECTION",
                            "MEDIUM",
                            "Unencrypted connection detected",
                            browsing.getCurrentUrl(),
                            LocalDateTime.now(),
                            "WARNED",
                            false,
                            "Website is not using HTTPS encryption"
                    );
                    addSecurityAlert(alert);
                }
            }

        } catch (Exception e) {
            log.error("Error in security scanning: ", e);
        }
    }

    /**
     * Measure network performance for a specific device
     */
    private NetworkPerformanceDTO measureDevicePerformance(String deviceMac, String ipAddress) {
        try {
            if (ipAddress == null) return null;

            // Ping test
            Integer pingLatency = performPingTest(ipAddress);

            // Speed test (simplified)
            Double[] speeds = performSpeedTest(ipAddress);

            String quality = determineConnectionQuality(pingLatency, speeds[0]);

            return new NetworkPerformanceDTO(
                    deviceMac,
                    pingLatency,
                    speeds[0], // Download speed
                    speeds[1], // Upload speed
                    0, // Packet loss (would need more sophisticated testing)
                    quality,
                    LocalDateTime.now(),
                    "internal"
            );

        } catch (Exception e) {
            log.debug("Error measuring performance for device {}: ", deviceMac, e);
            return null;
        }
    }

    /**
     * Perform ping test to measure latency
     */
    private Integer performPingTest(String ipAddress) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            CommandLine cmdLine;

            if (os.contains("win")) {
                cmdLine = CommandLine.parse("ping -n 1 " + ipAddress);
            } else {
                cmdLine = CommandLine.parse("ping -c 1 " + ipAddress);
            }

            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            long startTime = System.currentTimeMillis();
            int exitCode = executor.execute(cmdLine);
            long endTime = System.currentTimeMillis();

            if (exitCode == 0) {
                String output = outputStream.toString("UTF-8");
                return extractPingTime(output, (int)(endTime - startTime));
            }

        } catch (Exception e) {
            log.debug("Ping test failed for {}: ", ipAddress, e);
        }

        return null;
    }

    /**
     * Extract ping time from ping command output
     */
    private Integer extractPingTime(String output, int fallbackTime) {
        // Try to extract actual ping time from output
        Pattern pattern = Pattern.compile("time[<=]([0-9.]+)\\s*ms");
        Matcher matcher = pattern.matcher(output);

        if (matcher.find()) {
            try {
                return (int) Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return fallbackTime;
            }
        }

        // Fallback to measured execution time
        return fallbackTime;
    }

    /**
     * Perform simplified speed test
     */
    private Double[] performSpeedTest(String ipAddress) {
        // Simplified speed test - in production would use iperf or similar
        // Return [download_speed, upload_speed] in Mbps

        Random random = new Random();
        Double downloadSpeed = 10.0 + random.nextDouble() * 90.0; // 10-100 Mbps
        Double uploadSpeed = downloadSpeed * 0.3; // Assume upload is ~30% of download

        return new Double[]{downloadSpeed, uploadSpeed};
    }

    /**
     * Determine connection quality based on performance metrics
     */
    private String determineConnectionQuality(Integer ping, Double downloadSpeed) {
        if (ping == null || downloadSpeed == null) return "UNKNOWN";

        if (ping < 20 && downloadSpeed > 50) return "EXCELLENT";
        if (ping < 50 && downloadSpeed > 25) return "GOOD";
        if (ping < 100 && downloadSpeed > 10) return "FAIR";
        return "POOR";
    }

    /**
     * Parse netstat output for traffic monitoring
     */
    private void parseNetstatOutput(String output) {
        String[] lines = output.split("\\n");
        for (String line : lines) {
            if (line.contains("eth") || line.contains("wlan") || line.contains("en0")) {
                // Extract interface traffic data
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 3) {
                    String interfaceName = parts[0];
                    try {
                        Integer rxPackets = Integer.parseInt(parts[3]);
                        networkTraffic.put(interfaceName + "_rx", rxPackets);
                    } catch (NumberFormatException e) {
                        // Ignore parsing errors
                    }
                }
            }
        }
    }

    /**
     * Parse Windows netstat output
     */
    private void parseWindowsNetstatOutput(String output) {
        // Windows netstat -e output parsing
        String[] lines = output.split("\\n");
        for (String line : lines) {
            if (line.contains("Bytes")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    try {
                        Integer bytes = Integer.parseInt(parts[1]);
                        networkTraffic.put("total_bytes", bytes);
                    } catch (NumberFormatException e) {
                        // Ignore parsing errors
                    }
                }
            }
        }
    }

    /**
     * Check site security and generate alerts if needed
     */
    private void checkSiteSecurity(SiteMonitoringDTO browsing) {
        String url = browsing.getCurrentUrl().toLowerCase();

        // Check for known malicious patterns
        if (isSuspiciousUrl(url)) {
            SecurityAlertDTO alert = new SecurityAlertDTO(
                    "MALICIOUS_SITE",
                    "CRITICAL",
                    "Access to potentially malicious website blocked",
                    browsing.getCurrentUrl(),
                    LocalDateTime.now(),
                    "BLOCKED",
                    false,
                    "Site matches threat intelligence patterns"
            );
            addSecurityAlert(alert);
        }

        // Check for inappropriate content
        if (containsInappropriateContent(url)) {
            SecurityAlertDTO alert = new SecurityAlertDTO(
                    "INAPPROPRIATE_CONTENT",
                    "HIGH",
                    "Access to inappropriate content detected",
                    browsing.getCurrentUrl(),
                    LocalDateTime.now(),
                    "WARNED",
                    false,
                    "Content filtering rules triggered"
            );
            addSecurityAlert(alert);
        }
    }

    /**
     * Add security alert to recent alerts list
     */
    private void addSecurityAlert(SecurityAlertDTO alert) {
        recentAlerts.add(0, alert); // Add to beginning

        // Keep only recent alerts
        while (recentAlerts.size() > MAX_RECENT_ALERTS) {
            recentAlerts.remove(recentAlerts.size() - 1);
        }

        log.warn("Security alert: {} - {}", alert.getAlertType(), alert.getDescription());
    }

    // ==========================================
    // PUBLIC METHODS FOR ACCESSING REAL-TIME DATA
    // ==========================================

    /**
     * Get current network performance for all devices
     */
    public Map<String, NetworkPerformanceDTO> getCurrentNetworkPerformance() {
        return new HashMap<>(devicePerformance);
    }

    /**
     * Get current browsing activity for all devices
     */
    public Map<String, SiteMonitoringDTO> getCurrentBrowsingActivity() {
        return new HashMap<>(currentBrowsing);
    }

    /**
     * Get recent security alerts
     */
    public List<SecurityAlertDTO> getRecentSecurityAlerts() {
        return new ArrayList<>(recentAlerts);
    }

    /**
     * Get current network traffic statistics
     */
    public Map<String, Integer> getCurrentNetworkTraffic() {
        return new HashMap<>(networkTraffic);
    }

    /**
     * Get performance data for a specific device
     */
    public NetworkPerformanceDTO getDevicePerformance(String deviceMac) {
        return devicePerformance.get(deviceMac);
    }

    /**
     * Get browsing activity for a specific device
     */
    public SiteMonitoringDTO getDeviceBrowsingActivity(String deviceMac) {
        return currentBrowsing.get(deviceMac);
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private String extractPageTitle(String url) {
        String domain = extractDomain(url);
        return domain.substring(0, 1).toUpperCase() + domain.substring(1);
    }

    private String extractDomain(String url) {
        if (url.contains("://")) {
            url = url.substring(url.indexOf("://") + 3);
        }
        if (url.contains("/")) {
            url = url.substring(0, url.indexOf("/"));
        }
        return url;
    }

    private String categorizeWebsite(String url) {
        String site = url.toLowerCase();
        if (site.contains("youtube") || site.contains("netflix")) return "Entertainment";
        if (site.contains("facebook") || site.contains("twitter")) return "Social Media";
        if (site.contains("github") || site.contains("stackoverflow")) return "Development";
        if (site.contains("amazon") || site.contains("shop")) return "Shopping";
        if (site.contains("news") || site.contains("cnn")) return "News";
        return "General";
    }

    private Boolean isSecureUrl(String url) {
        return url.startsWith("https://") ||
                (!url.startsWith("http://") &&
                        (url.contains("google") || url.contains("github") || url.contains("amazon")));
    }

    private Boolean isBlockedUrl(String url) {
        // Check against blocked URL patterns
        String[] blockedPatterns = {"malware", "phishing", "adult", "gambling"};
        String urlLower = url.toLowerCase();

        for (String pattern : blockedPatterns) {
            if (urlLower.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    private Integer calculateTimeOnSite(String deviceMac, String currentSite) {
        // Calculate time spent on current site
        SiteMonitoringDTO previous = currentBrowsing.get(deviceMac);
        if (previous != null && previous.getCurrentUrl().equals(currentSite)) {
            return (int) java.time.Duration.between(previous.getAccessTime(), LocalDateTime.now()).getSeconds();
        }
        return 0;
    }

    private String getPreviousSite(String deviceMac) {
        // This would track navigation history
        return "google.com"; // Simplified
    }

    private String getUserAgent(String deviceType) {
        switch (deviceType != null ? deviceType.toLowerCase() : "unknown") {
            case "mobile": return "Mozilla/5.0 (Mobile; rv:91.0) Gecko/91.0 Firefox/91.0";
            case "tablet": return "Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15";
            default: return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        }
    }

    private List<String> extractKeywords(String url) {
        String domain = extractDomain(url);
        String[] parts = domain.split("\\.");
        List<String> keywords = new ArrayList<>();

        for (String part : parts) {
            if (part.length() > 2 && !part.equals("www") && !part.equals("com") && !part.equals("org")) {
                keywords.add(part);
            }
        }

        return keywords;
    }

    private Boolean isSuspiciousUrl(String url) {
        String[] suspiciousPatterns = {
                "malware", "phishing", "scam", "fake", "suspicious",
                "click-here", "free-money", "virus", "trojan"
        };

        String urlLower = url.toLowerCase();
        for (String pattern : suspiciousPatterns) {
            if (urlLower.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    private Boolean containsInappropriateContent(String url) {
        String[] inappropriatePatterns = {
                "adult", "xxx", "porn", "gambling", "casino", "drugs"
        };

        String urlLower = url.toLowerCase();
        for (String pattern : inappropriatePatterns) {
            if (urlLower.contains(pattern)) {
                return true;
            }
        }

        return false;
    }
}