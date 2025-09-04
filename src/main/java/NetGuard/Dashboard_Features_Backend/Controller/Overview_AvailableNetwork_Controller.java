package NetGuard.Dashboard_Features_Backend.Controller;

import NetGuard.Dashboard_Features_Backend.DTO.*;
import NetGuard.Dashboard_Features_Backend.Service.Overview_AvailableNetwork_service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/overview/networks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "http://localhost:3001",
                "http://127.0.0.1:3000",
                "http://localhost:5173",
                "http://localhost:4173"
        },
        allowCredentials = "true",
        allowedHeaders = {
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        },
        methods = {
                RequestMethod.GET,
                RequestMethod.POST,
                RequestMethod.PUT,
                RequestMethod.DELETE,
                RequestMethod.OPTIONS
        },
        maxAge = 3600
)
public class Overview_AvailableNetwork_Controller {

    private final Overview_AvailableNetwork_service networkService;

    /**
     * 🔥 ENHANCED: Get complete network overview data with comprehensive device discovery
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNetworkOverview() {
        try {
            log.info("📊 Fetching comprehensive network overview with device discovery");

            NetworkOverviewDTO overview = networkService.getNetworkOverview();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Network overview with device discovery completed successfully");
            response.put("data", overview);
            response.put("timestamp", System.currentTimeMillis());

            // Enhanced logging
            log.info("✅ Network overview retrieved: {} networks found, {} devices connected, currently connected to: {}",
                    overview.getAvailableNetworks().size(),
                    overview.getConnectedDevices().size(),
                    overview.getConnectedWifi());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching network overview: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve network overview: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 ENHANCED: Perform real-time WiFi network scan with better error handling
     */
    @PostMapping("/scan")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performWiFiScan() {
        try {
            log.info("🔍 Starting enhanced real-time WiFi network scan");

            CompletableFuture<List<AvailableNetworkDTO>> scanFuture = networkService.scanAvailableNetworks();

            // Wait for scan to complete with extended timeout for comprehensive scanning
            List<AvailableNetworkDTO> networks = scanFuture.get(20, java.util.concurrent.TimeUnit.SECONDS);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Enhanced WiFi scan completed successfully");
            response.put("data", networks);
            response.put("count", networks.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("scanDuration", "Real-time comprehensive scan");

            log.info("✅ Enhanced WiFi scan completed: {} networks discovered", networks.size());

            return ResponseEntity.ok(response);

        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("⏰ WiFi scan timed out after 20 seconds");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "WiFi scan timed out. Please try again.");
            errorResponse.put("error", "ScanTimeout");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);

        } catch (Exception e) {
            log.error("❌ Error during WiFi scan: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "WiFi scan failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 ENHANCED: Connect to a specific WiFi network with comprehensive device discovery
     */
    @PostMapping("/connect")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> connectToWiFiNetwork(@Valid @RequestBody NetworkConnectionRequestDTO request) {
        try {
            log.info("🔗 Attempting enhanced WiFi connection to: {} (secured: {})",
                    request.getSsid(), request.getPassword() != null && !request.getPassword().isEmpty());

            // Enhanced input validation
            if (request.getSsid() == null || request.getSsid().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Network name (SSID) is required");
                errorResponse.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Set default device name if not provided
            if (request.getDeviceName() == null || request.getDeviceName().trim().isEmpty()) {
                request.setDeviceName("NetGuard Device");
            }

            // Attempt connection with enhanced device discovery
            NetworkConnectionResponseDTO connectionResult = networkService.connectToNetwork(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", connectionResult.getSuccess());
            response.put("data", connectionResult);
            response.put("timestamp", System.currentTimeMillis());

            if (connectionResult.getSuccess()) {
                log.info("✅ Successfully connected to WiFi network: {}", request.getSsid());

                response.put("message", "Successfully connected to " + request.getSsid() + " and performed device discovery");
                response.put("connectionDetails", Map.of(
                        "ssid", request.getSsid(),
                        "assignedIp", connectionResult.getAssignedIp(),
                        "signalStrength", connectionResult.getSignalStrength(),
                        "connectedAt", connectionResult.getConnectedAt(),
                        "deviceDiscovery", "Enhanced device discovery completed"
                ));

                // 🔥 NEW: Provide immediate feedback about device discovery
                response.put("nextSteps", Map.of(
                        "action", "Device discovery in progress",
                        "recommendation", "Check the Devices tab to see all discovered network devices",
                        "refreshInterval", "Data refreshes automatically every 30 seconds"
                ));

            } else {
                log.warn("❌ Failed to connect to WiFi network: {} - {}", request.getSsid(), connectionResult.getMessage());
                response.put("message", connectionResult.getMessage());
            }

            HttpStatus status = connectionResult.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("❌ Error connecting to WiFi network: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Connection failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 NEW: Trigger immediate device discovery for current network
     */
    @PostMapping("/discover-devices")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerDeviceDiscovery() {
        try {
            log.info("🔍 Manual device discovery triggered");

            // Get current network
            NetworkOverviewDTO overview = networkService.getNetworkOverview();
            String connectedNetwork = overview.getConnectedWifi();

            if ("Not Connected".equals(connectedNetwork)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Not connected to any network. Please connect to a WiFi network first.");
                errorResponse.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Trigger immediate device discovery
            networkService.performImmediateDeviceDiscovery(connectedNetwork);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device discovery completed successfully for network: " + connectedNetwork);
            response.put("networkName", connectedNetwork);
            response.put("discoveryType", "Comprehensive device scan");
            response.put("timestamp", System.currentTimeMillis());
            response.put("recommendation", "Check the network overview or devices tab to see discovered devices");

            log.info("✅ Manual device discovery completed for network: {}", connectedNetwork);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error triggering device discovery: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Device discovery failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 ENHANCED: Disconnect from current WiFi network with cleanup
     */
    @PostMapping("/disconnect")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> disconnectFromWiFi(@RequestParam(required = false) String deviceMac) {
        try {
            log.info("🔌 Attempting to disconnect from current WiFi network");

            // Use a default device MAC if not provided
            if (deviceMac == null || deviceMac.trim().isEmpty()) {
                deviceMac = "auto-detect";
            }

            NetworkConnectionResponseDTO disconnectionResult = networkService.disconnectFromNetwork(deviceMac);

            Map<String, Object> response = new HashMap<>();
            response.put("success", disconnectionResult.getSuccess());
            response.put("message", disconnectionResult.getMessage());
            response.put("data", disconnectionResult);
            response.put("timestamp", System.currentTimeMillis());

            if (disconnectionResult.getSuccess()) {
                log.info("✅ Successfully disconnected from WiFi network");
                response.put("status", "Disconnected successfully");
                response.put("nextSteps", "All device connections have been cleared from the database");
            } else {
                log.warn("❌ Failed to disconnect from WiFi network: {}", disconnectionResult.getMessage());
            }

            HttpStatus status = disconnectionResult.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("❌ Error disconnecting from WiFi network: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Disconnection failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get only available networks from last scan (cached results)
     */
    @GetMapping("/available")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAvailableNetworks() {
        try {
            log.info("📋 Fetching cached available networks");

            // This returns cached scan results instead of performing new scan
            NetworkOverviewDTO overview = networkService.getNetworkOverview();
            List<AvailableNetworkDTO> networks = overview.getAvailableNetworks();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Available networks retrieved from cache");
            response.put("data", networks);
            response.put("count", networks.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("note", "Use /scan endpoint to refresh the list");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching available networks: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve available networks: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 ENHANCED: Refresh network data manually with comprehensive discovery
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshNetworkData() {
        try {
            log.info("🔄 Manual network data refresh with device discovery requested");

            // Trigger async network scan
            CompletableFuture<List<AvailableNetworkDTO>> scanFuture = networkService.scanAvailableNetworks();

            // Get current network for device discovery
            NetworkOverviewDTO overview = networkService.getNetworkOverview();
            String connectedNetwork = overview.getConnectedWifi();

            // If connected, also trigger device discovery
            if (!"Not Connected".equals(connectedNetwork)) {
                networkService.performImmediateDeviceDiscovery(connectedNetwork);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Network refresh and device discovery initiated successfully");
            response.put("status", "scanning");
            response.put("connectedNetwork", connectedNetwork);
            response.put("deviceDiscovery", !"Not Connected".equals(connectedNetwork) ? "Completed" : "Skipped (not connected)");
            response.put("timestamp", System.currentTimeMillis());
            response.put("note", "Scan is running in background. Use /overview to get updated results.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error refreshing network data: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to refresh network data: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 ENHANCED: Get current connection status with device count
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentConnectionStatus() {
        try {
            log.info("📊 Fetching current WiFi connection status with device information");

            NetworkOverviewDTO overview = networkService.getNetworkOverview();

            Map<String, Object> connectionStatus = new HashMap<>();
            connectionStatus.put("connectedNetwork", overview.getConnectedWifi());
            connectionStatus.put("isConnected", !overview.getConnectedWifi().equals("Not Connected"));
            connectionStatus.put("activeDevices", overview.getActiveDevices());
            connectionStatus.put("totalDevices", overview.getTotalDevices());
            connectionStatus.put("vpnActive", overview.getVpnActive());
            connectionStatus.put("discoveredDevices", overview.getConnectedDevices().size());

            // Get signal strength of connected network
            if (!overview.getConnectedWifi().equals("Not Connected")) {
                overview.getAvailableNetworks().stream()
                        .filter(network -> network.getName().equals(overview.getConnectedWifi()))
                        .findFirst()
                        .ifPresent(network -> {
                            connectionStatus.put("signalStrength", network.getSignal());
                            connectionStatus.put("frequency", network.getFrequency());
                            connectionStatus.put("security", network.getSecurity());
                        });
            }

            // Add device summary
            if (!overview.getConnectedDevices().isEmpty()) {
                connectionStatus.put("deviceSummary", Map.of(
                        "totalConnected", overview.getConnectedDevices().size(),
                        "recentlyConnected", overview.getConnectedDevices().stream()
                                .mapToLong(device -> device.getDataUsage() != null ? device.getDataUsage() : 0)
                                .sum(),
                        "lastDeviceConnected", overview.getConnectedDevices().stream()
                                .map(device -> device.getDeviceName())
                                .findFirst()
                                .orElse("Unknown")
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Connection status with device information retrieved successfully");
            response.put("data", connectionStatus);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching connection status: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve connection status: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for WiFi service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            // Basic system checks
            String os = System.getProperty("os.name");
            boolean wifiSupported = os.toLowerCase().contains("win") ||
                    os.toLowerCase().contains("mac") ||
                    os.toLowerCase().contains("linux");

            Map<String, Object> healthData = new HashMap<>();
            healthData.put("service", "Enhanced WiFi Network Scanner");
            healthData.put("status", "healthy");
            healthData.put("operatingSystem", os);
            healthData.put("wifiScanSupported", wifiSupported);
            healthData.put("deviceDiscoverySupported", true);
            healthData.put("version", "3.0.0 - Enhanced Real WiFi Scanner with Device Discovery");
            healthData.put("features", List.of(
                    "Real WiFi scanning",
                    "Comprehensive device discovery",
                    "Enhanced device naming",
                    "Automatic device monitoring",
                    "Cross-platform support"
            ));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Enhanced WiFi network service is healthy");
            response.put("data", healthData);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Health check failed: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 ENHANCED: Get detailed network statistics with device information
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNetworkStatistics() {
        try {
            log.info("📈 Fetching detailed network statistics with device analytics");

            NetworkOverviewDTO overview = networkService.getNetworkOverview();
            NetworkStatsDTO stats = overview.getNetworkStats();

            Map<String, Object> detailedStats = new HashMap<>();
            detailedStats.put("networkStats", stats);
            detailedStats.put("availableNetworksCount", overview.getAvailableNetworks().size());
            detailedStats.put("connectedNetworkName", overview.getConnectedWifi());
            detailedStats.put("dailyVisitedSites", overview.getDailyVisitedSites());
            detailedStats.put("discoveredDevicesCount", overview.getConnectedDevices().size());

            // Enhanced device analytics
            if (!overview.getConnectedDevices().isEmpty()) {
                Map<String, Object> deviceAnalytics = new HashMap<>();
                deviceAnalytics.put("totalDevices", overview.getConnectedDevices().size());
                deviceAnalytics.put("totalDataUsage", overview.getConnectedDevices().stream()
                        .mapToLong(device -> device.getDataUsage() != null ? device.getDataUsage() : 0)
                        .sum());
                deviceAnalytics.put("deviceTypes", overview.getConnectedDevices().stream()
                        .map(device -> device.getDeviceName().contains("Apple") ? "Apple" :
                                device.getDeviceName().contains("Samsung") ? "Samsung" :
                                        device.getDeviceName().contains("Router") ? "Network Equipment" : "Other")
                        .distinct()
                        .toList());

                detailedStats.put("deviceAnalytics", deviceAnalytics);
            }

            // Additional network statistics
            if (!overview.getAvailableNetworks().isEmpty()) {
                double maxSignal = overview.getAvailableNetworks().stream()
                        .mapToInt(AvailableNetworkDTO::getSignal)
                        .max().orElse(0);
                double minSignal = overview.getAvailableNetworks().stream()
                        .mapToInt(AvailableNetworkDTO::getSignal)
                        .min().orElse(0);

                detailedStats.put("strongestSignal", maxSignal);
                detailedStats.put("weakestSignal", minSignal);
                detailedStats.put("averageSignal", overview.getAvailableNetworks().stream()
                        .mapToInt(AvailableNetworkDTO::getSignal)
                        .average().orElse(0.0));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Enhanced network statistics retrieved successfully");
            response.put("data", detailedStats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching network statistics: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve network statistics: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Check if a specific network is in range
     */
    @GetMapping("/check/{ssid}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkNetworkAvailability(@PathVariable String ssid) {
        try {
            log.info("🔍 Checking availability of network: {}", ssid);

            NetworkOverviewDTO overview = networkService.getNetworkOverview();

            AvailableNetworkDTO targetNetwork = overview.getAvailableNetworks().stream()
                    .filter(network -> network.getName().equalsIgnoreCase(ssid))
                    .findFirst()
                    .orElse(null);

            Map<String, Object> checkResult = new HashMap<>();
            checkResult.put("ssid", ssid);
            checkResult.put("available", targetNetwork != null);
            checkResult.put("connected", targetNetwork != null && targetNetwork.getConnected());

            if (targetNetwork != null) {
                checkResult.put("signalStrength", targetNetwork.getSignal());
                checkResult.put("security", targetNetwork.getSecurity());
                checkResult.put("frequency", targetNetwork.getFrequency());
                checkResult.put("secured", targetNetwork.getSecured());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", targetNetwork != null ?
                    "Network found" : "Network not found in current scan");
            response.put("data", checkResult);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error checking network availability: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to check network availability: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}