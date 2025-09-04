package NetGuard.Dashboard_Features_Backend.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealWebsiteMonitoringService {

    private final Map<String, String> deviceCurrentSites = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastSiteUpdate = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> deviceSiteHistory = new ConcurrentHashMap<>();

    /**
     * 🔥 MAIN METHOD: Get current website for device
     */
    public String getCurrentWebsiteForDevice(String deviceMac, String deviceIp) {
        try {
            log.debug("🌐 Getting current website for device {} ({})", deviceMac, deviceIp);

            // Method 1: DNS Query Monitoring (Most Reliable)
            String dnsWebsite = getCurrentWebsiteFromDNS(deviceIp);
            if (dnsWebsite != null) {
                updateDeviceCurrentSite(deviceMac, dnsWebsite);
                return dnsWebsite;
            }

            // Method 2: Network Traffic Analysis
            String trafficWebsite = getCurrentWebsiteFromTraffic(deviceIp);
            if (trafficWebsite != null) {
                updateDeviceCurrentSite(deviceMac, trafficWebsite);
                return trafficWebsite;
            }

            // Method 3: Router API Integration
            String routerWebsite = getCurrentWebsiteFromRouter(deviceIp);
            if (routerWebsite != null) {
                updateDeviceCurrentSite(deviceMac, routerWebsite);
                return routerWebsite;
            }

            // Method 4: Browser Process Monitoring (if device is local)
            String browserWebsite = getCurrentWebsiteFromBrowser(deviceIp);
            if (browserWebsite != null) {
                updateDeviceCurrentSite(deviceMac, browserWebsite);
                return browserWebsite;
            }

            // Method 5: HTTP/HTTPS Proxy Logs
            String proxyWebsite = getCurrentWebsiteFromProxy(deviceIp);
            if (proxyWebsite != null) {
                updateDeviceCurrentSite(deviceMac, proxyWebsite);
                return proxyWebsite;
            }

            // Return cached result if available
            return deviceCurrentSites.get(deviceMac);

        } catch (Exception e) {
            log.error("❌ Error getting current website for device {}: ", deviceMac, e);
            return deviceCurrentSites.get(deviceMac);
        }
    }

    /**
     * 🔥 Method 1: DNS Query Monitoring
     */
    private String getCurrentWebsiteFromDNS(String deviceIp) {
        try {
            // Monitor DNS queries from the device
            String[] commands = {
                    "netstat -p UDP | findstr :53",                    // Windows DNS queries
                    "lsof -i UDP:53",                                   // Linux/Mac DNS queries
                    "tcpdump -i any -n 'port 53' -c 10",              // Packet capture DNS
                    "dig @" + getRouterIP() + " +trace"                // DNS trace
            };

            for (String command : commands) {
                try {
                    String result = executeCommand(command, 3);
                    String website = extractWebsiteFromDNSOutput(result, deviceIp);
                    if (website != null) {
                        log.info("🔍 DNS: Device {} is accessing {}", deviceIp, website);
                        return website;
                    }
                } catch (Exception e) {
                    log.debug("DNS monitoring command failed: {}", command);
                }
            }

            // Alternative: Parse system DNS cache
            return parseSystemDNSCache(deviceIp);

        } catch (Exception e) {
            log.debug("DNS monitoring failed for {}: {}", deviceIp, e.getMessage());
        }
        return null;
    }

    /**
     * 🔥 Method 2: Network Traffic Analysis
     */
    private String getCurrentWebsiteFromTraffic(String deviceIp) {
        try {
            // Monitor active network connections from the device
            String[] commands = {
                    "netstat -an | findstr " + deviceIp,               // Windows connections
                    "ss -tuln | grep " + deviceIp,                     // Linux connections
                    "lsof -i | grep " + deviceIp,                      // Mac connections
                    "netstat -i | grep " + deviceIp                    // Generic connections
            };

            for (String command : commands) {
                try {
                    String result = executeCommand(command, 2);
                    String website = extractWebsiteFromTrafficOutput(result, deviceIp);
                    if (website != null) {
                        log.info("📡 Traffic: Device {} is connected to {}", deviceIp, website);
                        return website;
                    }
                } catch (Exception e) {
                    log.debug("Traffic monitoring command failed: {}", command);
                }
            }

            // Alternative: Check ARP table for recent activity
            return analyzeARPActivity(deviceIp);

        } catch (Exception e) {
            log.debug("Traffic analysis failed for {}: {}", deviceIp, e.getMessage());
        }
        return null;
    }

    /**
     * 🔥 Method 3: Router API Integration
     */
    private String getCurrentWebsiteFromRouter(String deviceIp) {
        try {
            String routerIP = getRouterIP();
            if (routerIP == null) return null;

            // Try different router API endpoints
            String[] endpoints = {
                    "http://" + routerIP + "/api/device/traffic",       // Generic router API
                    "http://" + routerIP + "/cgi-bin/luci/admin/status", // OpenWrt
                    "http://" + routerIP + "/api/v1/device/" + deviceIp, // Modern routers
            };

            for (String endpoint : endpoints) {
                try {
                    String response = makeHTTPRequest(endpoint);
                    String website = extractWebsiteFromRouterResponse(response, deviceIp);
                    if (website != null) {
                        log.info("🏠 Router: Device {} is accessing {}", deviceIp, website);
                        return website;
                    }
                } catch (Exception e) {
                    log.debug("Router API call failed: {}", endpoint);
                }
            }

            // Alternative: SNMP query to router
            return getSNMPTrafficData(routerIP, deviceIp);

        } catch (Exception e) {
            log.debug("Router API integration failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 🔥 Method 4: Browser Process Monitoring (Local device)
     */
    private String getCurrentWebsiteFromBrowser(String deviceIp) {
        try {
            // Only works if this is the local device
            if (!isLocalDevice(deviceIp)) return null;

            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                return getWindowsBrowserActivity();
            } else if (os.contains("mac")) {
                return getMacBrowserActivity();
            } else if (os.contains("linux")) {
                return getLinuxBrowserActivity();
            }

        } catch (Exception e) {
            log.debug("Browser monitoring failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 🔥 Method 5: HTTP/HTTPS Proxy Monitoring
     */
    private String getCurrentWebsiteFromProxy(String deviceIp) {
        try {
            // Parse proxy logs if available
            String[] logFiles = {
                    "/var/log/squid/access.log",           // Squid proxy
                    "/var/log/nginx/access.log",           // Nginx proxy
                    "/tmp/proxy.log",                      // Custom proxy
                    "C:\\proxy\\logs\\access.log"         // Windows proxy
            };

            for (String logFile : logFiles) {
                try {
                    if (Files.exists(Paths.get(logFile))) {
                        String content = readLastLinesFromFile(logFile, 100);
                        String website = extractWebsiteFromProxyLog(content, deviceIp);
                        if (website != null) {
                            log.info("🔐 Proxy: Device {} accessed {}", deviceIp, website);
                            return website;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to read proxy log: {}", logFile);
                }
            }

        } catch (Exception e) {
            log.debug("Proxy monitoring failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 🔥 Scheduled Website Monitoring
     */
    @Scheduled(fixedRate = 10000) // Every 10 seconds
    @Async("networkTaskExecutor")
    public void performWebsiteMonitoring() {
        try {
            log.debug("🌐 Performing scheduled website monitoring...");

            // Get all active devices (this would come from your device service)
            List<String[]> activeDevices = getActiveDevices(); // [MAC, IP]

            for (String[] device : activeDevices) {
                String deviceMac = device[0];
                String deviceIp = device[1];

                String currentWebsite = getCurrentWebsiteForDevice(deviceMac, deviceIp);
                if (currentWebsite != null) {
                    updateDeviceSiteHistory(deviceMac, currentWebsite);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error in scheduled website monitoring: ", e);
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void updateDeviceCurrentSite(String deviceMac, String website) {
        deviceCurrentSites.put(deviceMac, website);
        lastSiteUpdate.put(deviceMac, LocalDateTime.now());
        log.debug("📝 Updated current site for {}: {}", deviceMac, website);
    }

    private void updateDeviceSiteHistory(String deviceMac, String website) {
        deviceSiteHistory.computeIfAbsent(deviceMac, k -> new ConcurrentHashMap<>())
                .merge(website, 1, Integer::sum);
    }

    private String executeCommand(String command, int timeoutSeconds) throws Exception {
        CommandLine cmdLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);

        executor.setWatchdog(new org.apache.commons.exec.ExecuteWatchdog(timeoutSeconds * 1000L));

        int exitCode = executor.execute(cmdLine);
        if (exitCode == 0) {
            return outputStream.toString("UTF-8");
        }

        throw new RuntimeException("Command failed");
    }

    private String extractWebsiteFromDNSOutput(String output, String deviceIp) {
        // Parse DNS query output to extract website domains
        Pattern pattern = Pattern.compile("([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            String domain = matcher.group(1);
            if (isValidWebsite(domain)) {
                return domain;
            }
        }
        return null;
    }

    private String extractWebsiteFromTrafficOutput(String output, String deviceIp) {
        // Parse network traffic output to extract destination websites
        // Look for ESTABLISHED connections to web ports (80, 443)
        String[] lines = output.split("\n");

        for (String line : lines) {
            if (line.contains(deviceIp) && (line.contains(":80") || line.contains(":443"))) {
                String remoteAddress = extractRemoteAddress(line);
                if (remoteAddress != null) {
                    try {
                        String hostname = InetAddress.getByName(remoteAddress).getHostName();
                        if (isValidWebsite(hostname)) {
                            return hostname;
                        }
                    } catch (Exception e) {
                        // Continue with next line
                    }
                }
            }
        }
        return null;
    }

    private String getWindowsBrowserActivity() {
        try {
            // Query Windows browser processes
            String command = "wmic process where \"name='chrome.exe' or name='firefox.exe' or name='msedge.exe'\" get commandline";
            String result = executeCommand(command, 3);
            return extractURLFromBrowserCommand(result);
        } catch (Exception e) {
            log.debug("Windows browser monitoring failed: {}", e.getMessage());
        }
        return null;
    }

    private String getMacBrowserActivity() {
        try {
            // Query Mac browser activity using osascript
            String script = "tell application \"Google Chrome\" to get URL of active tab of first window";
            String command = "osascript -e '" + script + "'";
            String result = executeCommand(command, 3);
            return extractDomainFromURL(result.trim());
        } catch (Exception e) {
            log.debug("Mac browser monitoring failed: {}", e.getMessage());
        }
        return null;
    }

    private String getLinuxBrowserActivity() {
        try {
            // Query Linux browser processes
            String command = "ps aux | grep -E '(chrome|firefox|browser)' | grep -v grep";
            String result = executeCommand(command, 3);
            return extractURLFromProcessList(result);
        } catch (Exception e) {
            log.debug("Linux browser monitoring failed: {}", e.getMessage());
        }
        return null;
    }

    private boolean isValidWebsite(String domain) {
        if (domain == null || domain.trim().isEmpty()) return false;
        if (domain.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return false; // IP address
        if (domain.length() < 4) return false;
        if (domain.contains("localhost") || domain.contains("127.0.0.1")) return false;
        return domain.contains(".");
    }

    private String extractDomainFromURL(String url) {
        if (url == null) return null;

        // Remove protocol
        url = url.replaceFirst("^https?://", "");

        // Extract domain
        int slashIndex = url.indexOf('/');
        if (slashIndex > 0) {
            url = url.substring(0, slashIndex);
        }

        return url;
    }

    public String getDeviceCurrentWebsite(String deviceMac) {
        return deviceCurrentSites.get(deviceMac);
    }

    public List<String> getDeviceMostVisitedSites(String deviceMac) {
        Map<String, Integer> siteHistory = deviceSiteHistory.get(deviceMac);
        if (siteHistory == null) return new ArrayList<>();

        return siteHistory.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }

    // Additional helper methods...
    private String getRouterIP() {
        // Implementation to get router IP
        return "192.168.1.1";
    }

    private List<String[]> getActiveDevices() {
        // This should integrate with your existing device service
        return new ArrayList<>();
    }

    private boolean isLocalDevice(String ip) {
        // Check if IP is local device
        return false;
    }

    private String makeHTTPRequest(String url) {
        // HTTP request implementation
        return null;
    }

    private String parseSystemDNSCache(String deviceIp) { return null; }
    private String analyzeARPActivity(String deviceIp) { return null; }
    private String extractWebsiteFromRouterResponse(String response, String deviceIp) { return null; }
    private String getSNMPTrafficData(String routerIP, String deviceIp) { return null; }
    private String readLastLinesFromFile(String filename, int lines) { return null; }
    private String extractWebsiteFromProxyLog(String content, String deviceIp) { return null; }
    private String extractRemoteAddress(String line) { return null; }
    private String extractURLFromBrowserCommand(String result) { return null; }
    private String extractURLFromProcessList(String result) { return null; }
}