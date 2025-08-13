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
     * Get complete network overview data including current connection status
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNetworkOverview() {
        try {
            log.info("Fetching real-time network overview data");

            NetworkOverviewDTO overview = networkService.getNetworkOverview();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Network overview data retrieved successfully");
            response.put("data", overview);
            response.put("timestamp", System.currentTimeMillis());

            log.info("Network overview retrieved: {} networks found, currently connected to: {}",
                    overview.getAvailableNetworks().size(), overview.getConnectedWifi());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching network overview: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve network overview: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Perform real-time WiFi network scan to discover all available networks
     */
    @PostMapping("/scan")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performWiFiScan() {
        try {
            log.info("Starting real-time WiFi network scan");

            CompletableFuture<List<AvailableNetworkDTO>> scanFuture = networkService.scanAvailableNetworks();

            // Wait for scan to complete (with timeout)
            List<AvailableNetworkDTO> networks = scanFuture.get(java.util.concurrent.TimeUnit.SECONDS.toSeconds(15),
                    java.util.concurrent.TimeUnit.SECONDS);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "WiFi scan completed successfully");
            response.put("data", networks);
            response.put("count", networks.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("scanDuration", "Real-time scan");

            log.info("WiFi scan completed: {} networks discovered", networks.size());

            return ResponseEntity.ok(response);

        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("WiFi scan timed out after 15 seconds");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "WiFi scan timed out. Please try again.");
            errorResponse.put("error", "ScanTimeout");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);

        } catch (Exception e) {
            log.error("Error during WiFi scan: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "WiFi scan failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Connect to a specific WiFi network with credentials
     */
    @PostMapping("/connect")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> connectToWiFiNetwork(@Valid @RequestBody NetworkConnectionRequestDTO request) {
        try {
            log.info("Attempting to connect to WiFi network: {} (secured: {})",
                    request.getSsid(), request.getPassword() != null && !request.getPassword().isEmpty());

            // Validate input
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

            NetworkConnectionResponseDTO connectionResult = networkService.connectToNetwork(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", connectionResult.getSuccess());
            response.put("data", connectionResult);
            response.put("timestamp", System.currentTimeMillis());

            if (connectionResult.getSuccess()) {
                log.info("Successfully connected to WiFi network: {}", request.getSsid());

                // 🔥 NEW: Scan for all devices after successful connection
                try {
                    networkService.scanAllConnectedDevices();
                    response.put("message", "Successfully connected to " + request.getSsid() + " and scanning for all devices on network...");
                    log.info("Initiated device discovery scan for network: {}", request.getSsid());
                } catch (Exception scanError) {
                    log.warn("Connected successfully but device scan failed: {}", scanError.getMessage());
                    response.put("message", "Successfully connected to " + request.getSsid() + " (device scan failed)");
                }

                response.put("connectionDetails", Map.of(
                        "ssid", request.getSsid(),
                        "assignedIp", connectionResult.getAssignedIp(),
                        "signalStrength", connectionResult.getSignalStrength(),
                        "connectedAt", connectionResult.getConnectedAt()
                ));
            } else {
                log.warn("Failed to connect to WiFi network: {} - {}", request.getSsid(), connectionResult.getMessage());
                response.put("message", connectionResult.getMessage());
            }

            HttpStatus status = connectionResult.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Error connecting to WiFi network: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Connection failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * Disconnect from current WiFi network
     */
    @PostMapping("/disconnect")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> disconnectFromWiFi(@RequestParam(required = false) String deviceMac) {
        try {
            log.info("Attempting to disconnect from current WiFi network");

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
                log.info("Successfully disconnected from WiFi network");
            } else {
                log.warn("Failed to disconnect from WiFi network: {}", disconnectionResult.getMessage());
            }

            HttpStatus status = disconnectionResult.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Error disconnecting from WiFi network: ", e);

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
            log.info("Fetching cached available networks");

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
            log.error("Error fetching available networks: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve available networks: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Refresh network data manually (force scan + overview update)
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshNetworkData() {
        try {
            log.info("Manual network data refresh requested");

            // Trigger async network scan
            CompletableFuture<List<AvailableNetworkDTO>> scanFuture = networkService.scanAvailableNetworks();

            // Wait briefly for scan to start, then return
            Thread.sleep(1000);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Network refresh initiated successfully");
            response.put("status", "scanning");
            response.put("timestamp", System.currentTimeMillis());
            response.put("note", "Scan is running in background. Use /overview to get updated results.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error refreshing network data: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to refresh network data: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get current connection status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentConnectionStatus() {
        try {
            log.info("Fetching current WiFi connection status");

            NetworkOverviewDTO overview = networkService.getNetworkOverview();

            Map<String, Object> connectionStatus = new HashMap<>();
            connectionStatus.put("connectedNetwork", overview.getConnectedWifi());
            connectionStatus.put("isConnected", !overview.getConnectedWifi().equals("Not Connected"));
            connectionStatus.put("activeDevices", overview.getActiveDevices());
            connectionStatus.put("totalDevices", overview.getTotalDevices());
            connectionStatus.put("vpnActive", overview.getVpnActive());

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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Connection status retrieved successfully");
            response.put("data", connectionStatus);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching connection status: ", e);

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
            healthData.put("service", "WiFi Network Scanner");
            healthData.put("status", "healthy");
            healthData.put("operatingSystem", os);
            healthData.put("wifiScanSupported", wifiSupported);
            healthData.put("version", "2.0.0 - Real WiFi Scanner");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "WiFi network service is healthy");
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
     * Get detailed network statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNetworkStatistics() {
        try {
            log.info("Fetching detailed network statistics");

            NetworkOverviewDTO overview = networkService.getNetworkOverview();
            NetworkStatsDTO stats = overview.getNetworkStats();

            Map<String, Object> detailedStats = new HashMap<>();
            detailedStats.put("networkStats", stats);
            detailedStats.put("availableNetworksCount", overview.getAvailableNetworks().size());
            detailedStats.put("connectedNetworkName", overview.getConnectedWifi());
            detailedStats.put("dailyVisitedSites", overview.getDailyVisitedSites());

            // Additional statistics
            if (!overview.getAvailableNetworks().isEmpty()) {
                double maxSignal = overview.getAvailableNetworks().stream()
                        .mapToInt(AvailableNetworkDTO::getSignal)
                        .max().orElse(0);
                double minSignal = overview.getAvailableNetworks().stream()
                        .mapToInt(AvailableNetworkDTO::getSignal)
                        .min().orElse(0);

                detailedStats.put("strongestSignal", maxSignal);
                detailedStats.put("weakestSignal", minSignal);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Network statistics retrieved successfully");
            response.put("data", detailedStats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching network statistics: ", e);

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
            log.info("Checking availability of network: {}", ssid);

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
            log.error("Error checking network availability: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to check network availability: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}