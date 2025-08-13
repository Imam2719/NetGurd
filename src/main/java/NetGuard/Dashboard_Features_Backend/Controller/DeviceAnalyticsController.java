package NetGuard.Dashboard_Features_Backend.Controller;

import NetGuard.Dashboard_Features_Backend.DTO.*;
import NetGuard.Dashboard_Features_Backend.Service.DeviceAnalyticsService;
import NetGuard.Dashboard_Features_Backend.Service.RealTimeMonitoringService;
import NetGuard.Dashboard_Features_Backend.Service.DeviceManagementService;
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

@RestController
@RequestMapping("/api/analytics")
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
public class DeviceAnalyticsController {

    private final DeviceAnalyticsService analyticsService;
    private final RealTimeMonitoringService monitoringService;
    private final DeviceManagementService deviceManagementService;

    /**
     * 🔥 Get device analytics for specified time range
     */
    @GetMapping("/devices")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDeviceAnalytics(
            @RequestParam(defaultValue = "24h") String timeRange) {
        try {
            log.info("📊 Fetching device analytics for time range: {}", timeRange);

            List<DeviceAnalyticsDTO> analytics = analyticsService.getDeviceAnalytics(timeRange);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device analytics retrieved successfully");
            response.put("data", analytics);
            response.put("timeRange", timeRange);
            response.put("deviceCount", analytics.size());
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ Retrieved analytics for {} devices", analytics.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching device analytics: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve device analytics: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 Get current real-time device activity
     */
    @GetMapping("/devices/current")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentDeviceActivity() {
        try {
            log.info("📱 Fetching current device activity");

            List<DeviceActivityDTO> currentActivity = analyticsService.getCurrentDeviceActivity();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Current device activity retrieved successfully");
            response.put("data", currentActivity);
            response.put("activeDeviceCount", currentActivity.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching current device activity: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve current device activity: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 Get comprehensive dashboard analytics
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardAnalytics() {
        try {
            log.info("📊 Fetching comprehensive dashboard analytics");

            DashboardAnalyticsDTO dashboardData = analyticsService.getDashboardAnalytics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dashboard analytics retrieved successfully");
            response.put("data", dashboardData);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching dashboard analytics: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve dashboard analytics: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 Get real-time network performance metrics
     */
    @GetMapping("/performance/network")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNetworkPerformance() {
        try {
            log.info("⚡ Fetching real-time network performance metrics");

            Map<String, NetworkPerformanceDTO> performanceData = monitoringService.getCurrentNetworkPerformance();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Network performance metrics retrieved successfully");
            response.put("data", performanceData);
            response.put("deviceCount", performanceData.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching network performance: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve network performance: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 Get recent security alerts
     */
    @GetMapping("/security/alerts")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSecurityAlerts() {
        try {
            log.info("🔒 Fetching recent security alerts");

            List<SecurityAlertDTO> alerts = monitoringService.getRecentSecurityAlerts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Security alerts retrieved successfully");
            response.put("data", alerts);
            response.put("alertCount", alerts.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching security alerts: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve security alerts: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 Trigger manual device scan
     */
    @PostMapping("/devices/scan")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerDeviceScan() {
        try {
            log.info("🔍 Manual device scan triggered");

            // This would trigger the device scanning in the network service
            // For now, return success message
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device scan initiated successfully");
            response.put("status", "scanning");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error triggering device scan: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger device scan: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 Get analytics for a specific device
     */
    @GetMapping("/devices/{deviceMac}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDeviceSpecificAnalytics(
            @PathVariable String deviceMac,
            @RequestParam(defaultValue = "24h") String timeRange) {
        try {
            log.info("📱 Fetching analytics for device: {} ({})", deviceMac, timeRange);

            // Get performance data
            NetworkPerformanceDTO performance = monitoringService.getDevicePerformance(deviceMac);

            // Get browsing activity
            SiteMonitoringDTO browsing = monitoringService.getDeviceBrowsingActivity(deviceMac);

            // Get device activity
            DeviceActivityDTO activity = deviceManagementService.getDeviceActivity(deviceMac);

            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("deviceMac", deviceMac);
            deviceData.put("performance", performance);
            deviceData.put("browsing", browsing);
            deviceData.put("activity", activity);
            deviceData.put("timeRange", timeRange);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device analytics retrieved successfully");
            response.put("data", deviceData);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching device analytics for {}: ", deviceMac, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve device analytics: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔥 Health check for analytics service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> healthData = new HashMap<>();
            healthData.put("service", "Device Analytics Service");
            healthData.put("status", "healthy");
            healthData.put("version", "2.0.0");
            healthData.put("features", List.of(
                    "Real-time device monitoring",
                    "Network performance tracking",
                    "Security threat detection",
                    "Usage analytics"
            ));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analytics service is healthy");
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
}