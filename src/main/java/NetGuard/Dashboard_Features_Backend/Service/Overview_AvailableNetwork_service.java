package NetGuard.Dashboard_Features_Backend.Service;

import NetGuard.Dashboard_Features_Backend.DTO.*;
import NetGuard.Dashboard_Features_Backend.Entity.AvailableNetwork;
import NetGuard.Dashboard_Features_Backend.Entity.NetworkConnection;
import NetGuard.Dashboard_Features_Backend.Repository.AvailableNetworkRepository;
import NetGuard.Dashboard_Features_Backend.Repository.NetworkConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Overview_AvailableNetwork_service {

    private final AvailableNetworkRepository networkRepository;
    private final NetworkConnectionRepository connectionRepository;

    /**
     * Scan for available networks using system commands - REAL WiFi scanning
     */
    @Async
    public CompletableFuture<List<AvailableNetworkDTO>> scanAvailableNetworks() {
        try {
            log.info("Starting REAL WiFi network scan...");
            List<AvailableNetwork> networks = performRealWiFiScan();

            // Clear old scan results and save new ones
            markOldNetworksAsUnavailable();

            // Save or update networks in database
            for (AvailableNetwork network : networks) {
                saveOrUpdateScannedNetwork(network);
            }

            List<AvailableNetworkDTO> networkDTOs = networks.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("WiFi scan completed. Found {} networks", networkDTOs.size());
            return CompletableFuture.completedFuture(networkDTOs);

        } catch (Exception e) {
            log.error("Error during WiFi scan: ", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    /**
     * Get network overview data with current connection status
     */
    @Transactional(readOnly = true)
    public NetworkOverviewDTO getNetworkOverview() {
        try {
            // Get currently connected network info
            String connectedWifi = getCurrentlyConnectedNetwork();

            // Get available networks from recent scan
            List<AvailableNetwork> availableNetworks = networkRepository.findByIsAvailableTrue();

            // Mark the currently connected network
            availableNetworks.forEach(network -> network.setIsConnected(network.getSsid().equals(connectedWifi)));

            // Get active connections
            List<NetworkConnection> activeConnections = connectionRepository.findByIsCurrentlyConnectedTrue();

            // Calculate statistics
            NetworkStatsDTO stats = calculateNetworkStats(availableNetworks, activeConnections);

            NetworkOverviewDTO overview = new NetworkOverviewDTO();
            overview.setConnectedWifi(connectedWifi);
            overview.setTotalDevices(getConnectedDeviceCount());
            overview.setActiveDevices(activeConnections.size());
            overview.setTotalTimeUsed(calculateTotalTimeUsed(activeConnections));
            overview.setVpnActive(isVpnActive());
            overview.setDailyVisitedSites(calculateDailyVisitedSites());
            overview.setAvailableNetworks(availableNetworks.stream()
                    .map(this::convertToDTO).collect(Collectors.toList()));
            overview.setConnectedDevices(activeConnections.stream()
                    .map(this::convertToConnectedDeviceDTO).collect(Collectors.toList()));
            overview.setNetworkStats(stats);

            return overview;

        } catch (Exception e) {
            log.error("Error getting network overview: ", e);
            return new NetworkOverviewDTO();
        }
    }

    /**
     * Connect to a WiFi network with password
     */
    @Transactional
    public NetworkConnectionResponseDTO connectToNetwork(NetworkConnectionRequestDTO request) {
        try {
            log.info("Attempting to connect to WiFi network: {}", request.getSsid());

            // Find the network from scan results
            Optional<AvailableNetwork> networkOpt = networkRepository.findBySsid(request.getSsid());
            if (networkOpt.isEmpty()) {
                return new NetworkConnectionResponseDTO(false,
                        "Network '" + request.getSsid() + "' not found. Please scan for networks first.",
                        null, null, null);
            }

            AvailableNetwork network = networkOpt.get();

            // Check if network requires password
            if (network.getIsSecured() && (request.getPassword() == null || request.getPassword().trim().isEmpty())) {
                return new NetworkConnectionResponseDTO(false,
                        "This network is secured and requires a password",
                        null, null, null);
            }

            // Disconnect from current network first
            disconnectFromCurrentNetwork();

            // Attempt connection using system commands
            boolean connected = attemptWiFiConnection(request.getSsid(), request.getPassword(), network.getIsSecured());

            if (connected) {
                // Wait a moment for connection to establish
                Thread.sleep(3000);

                // Verify connection was successful
                String currentNetwork = getCurrentlyConnectedNetwork();
                if (request.getSsid().equals(currentNetwork)) {
                    // Update network status
                    network.setIsConnected(true);
                    networkRepository.save(network);

                    // Create connection record
                    NetworkConnection connection = new NetworkConnection();
                    connection.setNetwork(network);
                    connection.setDeviceName(request.getDeviceName());
                    connection.setDeviceMac(getCurrentDeviceMac());
                    connection.setAssignedIp(getCurrentIpAddress());
                    connection.setConnectedAt(LocalDateTime.now());
                    connection.setConnectionStatus("CONNECTED");
                    connection.setIsCurrentlyConnected(true);

                    connectionRepository.save(connection);

                    return new NetworkConnectionResponseDTO(
                            true,
                            "Successfully connected to " + request.getSsid(),
                            connection.getAssignedIp(),
                            network.getSignalStrength(),
                            connection.getConnectedAt()
                    );
                } else {
                    return new NetworkConnectionResponseDTO(
                            false,
                            "Connection attempt completed but verification failed. Please check password and try again.",
                            null, null, null
                    );
                }
            } else {
                return new NetworkConnectionResponseDTO(
                        false,
                        "Failed to connect to " + request.getSsid() + ". Please check the password and signal strength.",
                        null, null, null
                );
            }

        } catch (Exception e) {
            log.error("Error connecting to WiFi network: ", e);
            return new NetworkConnectionResponseDTO(
                    false,
                    "Connection error: " + e.getMessage(),
                    null, null, null
            );
        }
    }

    /**
     * 🔥 ENHANCED METHOD: Scan network for ALL connected devices with better names
     */
    @Async
    public void scanAllConnectedDevices() {
        try {
            log.info("🔍 Scanning network for ALL connected devices with enhanced detection...");

            String currentNetwork = getCurrentlyConnectedNetwork();
            if ("Not Connected".equals(currentNetwork)) {
                log.warn("Not connected to any network, skipping device scan");
                return;
            }

            // Find the network in database
            Optional<AvailableNetwork> networkOpt = networkRepository.findBySsid(currentNetwork);
            if (networkOpt.isEmpty()) {
                log.warn("Network {} not found in database", currentNetwork);
                return;
            }
            AvailableNetwork network = networkOpt.get();

            // Clear old connections for this network first
            clearOldNetworkConnections(network);

            // Discover devices using ARP table (most reliable method)
            List<String[]> discoveredDevices = scanArpTableForAllDevices();

            log.info("📱 Found {} devices in ARP table", discoveredDevices.size());

            // Save each discovered device with enhanced naming
            for (String[] deviceInfo : discoveredDevices) {
                String ip = deviceInfo[0];
                String mac = deviceInfo[1];
                String enhancedDeviceName = getEnhancedDeviceName(mac, ip);

                saveNetworkDevice(network, enhancedDeviceName, mac, ip);
            }

            log.info("✅ Enhanced device discovery completed for network: {}", currentNetwork);

        } catch (Exception e) {
            log.error("❌ Error during enhanced device discovery: ", e);
        }
    }

    /**
     * Disconnect from current network
     */
    @Transactional
    public NetworkConnectionResponseDTO disconnectFromNetwork(String deviceMac) {
        try {
            String currentNetwork = getCurrentlyConnectedNetwork();
            if ("Not Connected".equals(currentNetwork)) {
                return new NetworkConnectionResponseDTO(
                        false,
                        "No network connection found to disconnect",
                        null, null, null
                );
            }

            // Disconnect using system command
            boolean disconnected = disconnectFromCurrentNetwork();

            if (disconnected) {
                // Update database records
                List<NetworkConnection> activeConnections = connectionRepository.findByIsCurrentlyConnectedTrue();
                for (NetworkConnection connection : activeConnections) {
                    connection.setIsCurrentlyConnected(false);
                    connection.setDisconnectedAt(LocalDateTime.now());
                    connection.setConnectionStatus("DISCONNECTED");
                    connectionRepository.save(connection);
                }

                // Update network status
                Optional<AvailableNetwork> networkOpt = networkRepository.findBySsid(currentNetwork);
                if (networkOpt.isPresent()) {
                    AvailableNetwork network = networkOpt.get();
                    network.setIsConnected(false);
                    networkRepository.save(network);
                }

                return new NetworkConnectionResponseDTO(
                        true,
                        "Successfully disconnected from " + currentNetwork,
                        null, null, LocalDateTime.now()
                );
            } else {
                return new NetworkConnectionResponseDTO(
                        false,
                        "Failed to disconnect from network",
                        null, null, null
                );
            }

        } catch (Exception e) {
            log.error("Error disconnecting from network: ", e);
            return new NetworkConnectionResponseDTO(
                    false,
                    "Disconnection error: " + e.getMessage(),
                    null, null, null
            );
        }
    }

    /**
     * Scheduled task to refresh network data every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void refreshNetworkData() {
        log.debug("Refreshing WiFi network data...");
        scanAvailableNetworks();
        updateConnectionStatuses();
    }

    // ===========================================
    // 🔥 ENHANCED DEVICE DISCOVERY METHODS
    // ===========================================

    /**
     * Scan ARP table to find ALL devices on current network
     */
    private List<String[]> scanArpTableForAllDevices() {
        List<String[]> devices = new ArrayList<>();

        try {
            String os = System.getProperty("os.name").toLowerCase();
            CommandLine cmdLine;

            if (os.contains("win")) {
                cmdLine = CommandLine.parse("arp -a");
            } else if (os.contains("mac")) {
                cmdLine = CommandLine.parse("arp -a");
            } else { // Linux
                cmdLine = CommandLine.parse("ip neigh show");
            }

            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            int exitCode = executor.execute(cmdLine);
            if (exitCode == 0) {
                String output = outputStream.toString("UTF-8");
                devices = parseArpTableOutput(output, os);
            }

        } catch (Exception e) {
            log.debug("ARP scan failed, trying alternative method: ", e);
            // Fallback: try ping sweep
            devices = performPingSweep();
        }

        return devices;
    }

    /**
     * Parse ARP table output to extract device information
     */
    private List<String[]> parseArpTableOutput(String output, String os) {
        List<String[]> devices = new ArrayList<>();
        String[] lines = output.split("\\n");

        for (String line : lines) {
            try {
                if (os.contains("win")) {
                    // Windows: "192.168.1.5        aa-bb-cc-dd-ee-ff     dynamic"
                    Pattern pattern = Pattern.compile("\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+([0-9a-fA-F-]{17})\\s+dynamic");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String ip = matcher.group(1);
                        String mac = matcher.group(2).replace("-", ":");
                        devices.add(new String[]{ip, mac, ""});
                    }
                } else {
                    // Linux/Mac: "192.168.1.5 dev wlan0 lladdr aa:bb:cc:dd:ee:ff REACHABLE"
                    Pattern pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+).*?([0-9a-fA-F:]{17})");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String ip = matcher.group(1);
                        String mac = matcher.group(2);
                        devices.add(new String[]{ip, mac, ""});
                    }
                }
            } catch (Exception e) {
                // Skip invalid lines
            }
        }

        log.info("Parsed {} devices from ARP table", devices.size());
        return devices;
    }

    /**
     * 🔥 ENHANCED: Get enhanced device name using multiple detection methods
     */
    private String getEnhancedDeviceName(String mac, String ip) {
        try {
            log.debug("🔍 Detecting device name for IP: {} MAC: {}", ip, mac);

            // Method 1: Try to get real hostname first (most accurate)
            String hostname = getEnhancedHostname(ip);
            if (hostname != null && !hostname.startsWith("Device ") && !hostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                log.info("✅ Found hostname: {} for {}", hostname, ip);
                return cleanHostname(hostname);
            }

            // Method 2: Enhanced MAC vendor identification
            String vendorDevice = identifyDeviceByMACVendor(mac, ip);
            if (vendorDevice != null && !vendorDevice.startsWith("Network Device")) {
                log.info("✅ Identified by MAC vendor: {} for {}", vendorDevice, ip);
                return vendorDevice;
            }

            // Method 3: Device type detection by behavior
            String behaviorDevice = detectDeviceTypeByBehavior(ip, mac);
            if (behaviorDevice != null) {
                log.info("✅ Identified by behavior: {} for {}", behaviorDevice, ip);
                return behaviorDevice;
            }

            // Fallback: Generic name with IP
            String fallbackName = "Network Device (" + ip.substring(ip.lastIndexOf('.') + 1) + ")";
            log.debug("Using fallback name: {} for {}", fallbackName, ip);
            return fallbackName;

        } catch (Exception e) {
            log.debug("Error detecting device name for {}: {}", ip, e.getMessage());
            return "Network Device (" + ip.substring(ip.lastIndexOf('.') + 1) + ")";
        }
    }

    /**
     * Enhanced hostname resolution with multiple methods
     */
    private String getEnhancedHostname(String ip) {
        try {
            // Method 1: Standard Java hostname resolution
            InetAddress addr = InetAddress.getByName(ip);
            String hostname = addr.getHostName();

            if (!hostname.equals(ip) && !hostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                return hostname;
            }

            // Method 2: Try system-specific hostname lookup
            String systemHostname = getSystemHostname(ip);
            if (systemHostname != null && !systemHostname.equals(ip)) {
                return systemHostname;
            }

            // Method 3: Try NetBIOS name (Windows devices)
            String netbiosName = getNetBIOSName(ip);
            if (netbiosName != null && !netbiosName.equals(ip)) {
                return netbiosName;
            }

        } catch (Exception e) {
            log.debug("Error resolving hostname for {}: {}", ip, e.getMessage());
        }

        return null;
    }

    /**
     * Enhanced MAC vendor database with comprehensive device identification
     */
    private String identifyDeviceByMACVendor(String mac, String ip) {
        if (mac == null || mac.isEmpty()) {
            return null;
        }

        // Get expanded MAC vendor database
        Map<String, String> macVendors = getExpandedMACVendorMap();

        // Clean and normalize MAC address
        String macUpper = mac.toUpperCase().replace(":", "").replace("-", "");

        // Check for exact OUI match (first 6 characters)
        if (macUpper.length() >= 6) {
            String oui = macUpper.substring(0, 6);
            if (macVendors.containsKey(oui)) {
                String vendor = macVendors.get(oui);
                return formatDeviceName(vendor, ip);
            }
        }

        // Check for partial matches for better identification
        for (Map.Entry<String, String> entry : macVendors.entrySet()) {
            if (macUpper.startsWith(entry.getKey())) {
                String vendor = entry.getValue();
                return formatDeviceName(vendor, ip);
            }
        }

        // Router/Gateway detection
        if (ip.endsWith(".1") || ip.endsWith(".254")) {
            return "Router/Gateway (" + ip.substring(ip.lastIndexOf('.') + 1) + ")";
        }

        return null;
    }

    /**
     * Comprehensive MAC vendor database
     */
    private Map<String, String> getExpandedMACVendorMap() {
        Map<String, String> vendors = new HashMap<>();

        // Apple devices
        vendors.put("001B63", "Apple");
        vendors.put("28F076", "Apple");
        vendors.put("B8E856", "Apple");
        vendors.put("3C22FB", "Apple");
        vendors.put("A4C361", "Apple");
        vendors.put("8C2937", "Apple");
        vendors.put("DC86D8", "Apple");
        vendors.put("E0F847", "Apple");
        vendors.put("90B21F", "Apple");
        vendors.put("F0DBE2", "Apple");
        vendors.put("6C40F6", "Apple");

        // Samsung devices
        vendors.put("002312", "Samsung");
        vendors.put("34BE00", "Samsung");
        vendors.put("78F882", "Samsung");
        vendors.put("C06599", "Samsung");
        vendors.put("E8E5D6", "Samsung");
        vendors.put("442A60", "Samsung");
        vendors.put("7CF854", "Samsung");
        vendors.put("08EDB9", "Samsung");

        // Xiaomi devices
        vendors.put("342387", "Xiaomi");
        vendors.put("50EC50", "Xiaomi");
        vendors.put("78A3E4", "Xiaomi");
        vendors.put("AC84C6", "Xiaomi");
        vendors.put("040CCE", "Xiaomi");
        vendors.put("3400A5", "Xiaomi");

        // Huawei devices
        vendors.put("1C1B0D", "Huawei");
        vendors.put("E0C97A", "Huawei");
        vendors.put("480FCF", "Huawei");
        vendors.put("B4A5AC", "Huawei");
        vendors.put("002E5D", "Huawei");
        vendors.put("7824AF", "Huawei");

        // OnePlus devices
        vendors.put("AC3743", "OnePlus");
        vendors.put("A0821F", "OnePlus");

        // Google devices
        vendors.put("F4F5E8", "Google");
        vendors.put("DA7C02", "Google");
        vendors.put("CC3ADF", "Google");
        vendors.put("68C9A8", "Google");

        // Microsoft devices
        vendors.put("000D3A", "Microsoft");
        vendors.put("7C1E52", "Microsoft");
        vendors.put("E0CB4E", "Microsoft");
        vendors.put("9CB70D", "Microsoft");

        // LG devices
        vendors.put("001E75", "LG");
        vendors.put("10F96F", "LG");
        vendors.put("B0D09C", "LG");

        // Sony devices
        vendors.put("080046", "Sony");
        vendors.put("54724F", "Sony");
        vendors.put("984827", "Sony");

        // HP devices
        vendors.put("001A4B", "HP");
        vendors.put("009C02", "HP");
        vendors.put("6CAB31", "HP");

        // Dell devices
        vendors.put("001E4F", "Dell");
        vendors.put("B8AC6F", "Dell");
        vendors.put("84A9C4", "Dell");

        // Lenovo devices
        vendors.put("008CFA", "Lenovo");
        vendors.put("E41D2D", "Lenovo");
        vendors.put("4C80F1", "Lenovo");

        // Router manufacturers
        vendors.put("000C41", "Linksys");
        vendors.put("001F33", "Netgear");
        vendors.put("0050F2", "TP-Link");
        vendors.put("C4E90A", "TP-Link");
        vendors.put("E84E06", "TP-Link");
        vendors.put("001A2E", "D-Link");
        vendors.put("0017E2", "ASUS");
        vendors.put("2C4D54", "ASUS");

        // IoT and Smart devices
        vendors.put("B827EB", "Raspberry Pi");
        vendors.put("ECADB8", "Amazon Echo");
        vendors.put("747548", "Nintendo");
        vendors.put("001BC5", "Nintendo");

        return vendors;
    }

    /**
     * Try to get hostname using system commands
     */
    private String getSystemHostname(String ip) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            CommandLine cmdLine;

            if (os.contains("win")) {
                // Try nbtstat for NetBIOS name
                cmdLine = CommandLine.parse("nbtstat -A " + ip);
            } else {
                // Try host command on Unix systems
                cmdLine = CommandLine.parse("host " + ip);
            }

            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            // Set a short timeout for system commands
            executor.setWatchdog(new org.apache.commons.exec.ExecuteWatchdog(3000));

            int exitCode = executor.execute(cmdLine);
            if (exitCode == 0) {
                String output = outputStream.toString("UTF-8");
                return parseHostnameFromSystemOutput(output, os);
            }

        } catch (Exception e) {
            log.debug("System hostname lookup failed for {}: {}", ip, e.getMessage());
        }

        return null;
    }

    /**
     * Get NetBIOS name for Windows devices
     */
    private String getNetBIOSName(String ip) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                CommandLine cmdLine = CommandLine.parse("ping -a -n 1 " + ip);
                DefaultExecutor executor = new DefaultExecutor();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
                executor.setStreamHandler(streamHandler);

                executor.execute(cmdLine);
                String output = outputStream.toString("UTF-8");

                // Extract hostname from ping output
                Pattern pattern = Pattern.compile("Pinging\\s+([^\\s\\[]+)");
                Matcher matcher = pattern.matcher(output);
                if (matcher.find()) {
                    String hostname = matcher.group(1);
                    if (!hostname.equals(ip) && !hostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        return hostname;
                    }
                }
            }

        } catch (Exception e) {
            log.debug("NetBIOS lookup failed for {}: {}", ip, e.getMessage());
        }

        return null;
    }

    /**
     * Parse hostname from system command output
     */
    private String parseHostnameFromSystemOutput(String output, String os) {
        if (output == null || output.trim().isEmpty()) {
            return null;
        }

        if (os.contains("win")) {
            // Parse nbtstat output for NetBIOS name
            String[] lines = output.split("\\n");
            for (String line : lines) {
                if (line.contains("<00>") && line.contains("UNIQUE")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 0 && !parts[0].matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        return parts[0];
                    }
                }
            }
        } else {
            // Parse host command output
            if (output.contains("domain name pointer")) {
                String[] parts = output.split("domain name pointer");
                if (parts.length > 1) {
                    String hostname = parts[1].trim().replace(".", "");
                    if (!hostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        return hostname;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Detect device type by behavior and characteristics
     */
    private String detectDeviceTypeByBehavior(String ip, String mac) {
        try {
            // Check if device responds to common ports
            if (isHttpResponsive(ip, 80)) {
                if (ip.endsWith(".1") || ip.endsWith(".254")) {
                    return "Router/Gateway (" + ip.substring(ip.lastIndexOf('.') + 1) + ")";
                } else {
                    return "Web Device (" + ip.substring(ip.lastIndexOf('.') + 1) + ")";
                }
            }

            // Check for SSH (Linux/Unix devices)
            if (isPortResponsive(ip, 22)) {
                return "Linux Device (" + ip.substring(ip.lastIndexOf('.') + 1) + ")";
            }

            // Check for SMB (Windows devices)
            if (isPortResponsive(ip, 445)) {
                return "Windows Device (" + ip.substring(ip.lastIndexOf('.') + 1) + ")";
            }

            // Check if it's likely a mobile device
            if (isLikelyMobileDevice(mac)) {
                return "Mobile Device (" + ip.substring(ip.lastIndexOf('.') + 1) + ")";
            }

            return null;

        } catch (Exception e) {
            log.debug("Error detecting device behavior for {}: {}", ip, e.getMessage());
            return null;
        }
    }

    /**
     * Check if device responds to HTTP
     */
    private boolean isHttpResponsive(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if specific port is responsive
     */
    private boolean isPortResponsive(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if MAC suggests mobile device
     */
    private boolean isLikelyMobileDevice(String mac) {
        if (mac == null) return false;
        String macUpper = mac.toUpperCase().replace(":", "").replace("-", "");

        // Known mobile device MAC patterns
        String[] mobilePatterns = {
                "001B63", "28F076", "B8E856", // Apple mobile
                "002312", "34BE00", "78F882", // Samsung mobile
                "342387", "50EC50", "78A3E4", // Xiaomi mobile
                "AC3743", "A0821F"  // OnePlus mobile
        };

        for (String pattern : mobilePatterns) {
            if (macUpper.startsWith(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Format device name with vendor and IP
     */
    private String formatDeviceName(String vendor, String ip) {
        String deviceNumber = ip.substring(ip.lastIndexOf('.') + 1);

        // Add device type based on vendor
        switch (vendor.toLowerCase()) {
            case "apple":
                return "Apple Device (" + deviceNumber + ")";
            case "samsung":
                return "Samsung Device (" + deviceNumber + ")";
            case "xiaomi":
                return "Xiaomi Device (" + deviceNumber + ")";
            case "huawei":
                return "Huawei Device (" + deviceNumber + ")";
            case "google":
                return "Google Device (" + deviceNumber + ")";
            case "microsoft":
                return "Microsoft Device (" + deviceNumber + ")";
            case "raspberry pi":
                return "Raspberry Pi (" + deviceNumber + ")";
            case "linksys":
            case "netgear":
            case "tp-link":
            case "d-link":
            case "asus":
                return vendor + " Router (" + deviceNumber + ")";
            case "amazon echo":
                return "Amazon Echo (" + deviceNumber + ")";
            case "nintendo":
                return "Nintendo Device (" + deviceNumber + ")";
            default:
                return vendor + " Device (" + deviceNumber + ")";
        }
    }

    /**
     * Clean and format hostname
     */
    private String cleanHostname(String hostname) {
        if (hostname == null) return null;

        // Remove domain suffixes
        hostname = hostname.split("\\.")[0];

        // Clean up common patterns
        hostname = hostname.replace("_", " ");
        hostname = hostname.replace("-", " ");

        // Remove common prefixes/suffixes
        hostname = hostname.replaceAll("(?i)^(android|iphone|ipad|windows|linux|macos)-?", "");
        hostname = hostname.replaceAll("(?i)-?(android|iphone|ipad|windows|linux|macos)$", "");

        // Capitalize first letter of each word
        String[] words = hostname.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                if (result.length() > 0) result.append(" ");
                result.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }

        return result.length() > 0 ? result.toString() : hostname;
    }

    /**
     * Fallback: Ping sweep to find active devices
     */
    private List<String[]> performPingSweep() {
        List<String[]> devices = new ArrayList<>();

        try {
            String currentIP = getCurrentIpAddress();
            String[] ipParts = currentIP.split("\\.");
            String networkBase = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".";

            log.info("Performing ping sweep on network: {}", networkBase + "x");

            // Quick ping sweep (only check common IP ranges)
            int[] commonIPs = {1, 2, 3, 4, 5, 10, 20, 50, 100, 101, 102, 200, 254};

            for (int ip : commonIPs) {
                String testIP = networkBase + ip;
                if (pingDevice(testIP)) {
                    devices.add(new String[]{testIP, "Unknown", ""});
                }
            }

        } catch (Exception e) {
            log.debug("Ping sweep failed: ", e);
        }

        return devices;
    }

    /**
     * Check if device responds to ping
     */
    private boolean pingDevice(String ip) {
        try {
            InetAddress inet = InetAddress.getByName(ip);
            return inet.isReachable(1500); // 1.5 second timeout
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clear old connections for this network to avoid duplicates
     */
    @Transactional
    public void clearOldNetworkConnections(AvailableNetwork network) {
        try {
            List<NetworkConnection> oldConnections = connectionRepository.findByNetworkAndIsCurrentlyConnectedTrue(network);
            for (NetworkConnection conn : oldConnections) {
                conn.setIsCurrentlyConnected(false);
                conn.setDisconnectedAt(LocalDateTime.now());
                connectionRepository.save(conn);
            }
            log.info("Cleared {} old connections for network: {}", oldConnections.size(), network.getSsid());
        } catch (Exception e) {
            log.debug("Error clearing old connections: ", e);
        }
    }

    /**
     * Save discovered network device to database
     */
    @Transactional
    public void saveNetworkDevice(AvailableNetwork network, String deviceName, String mac, String ip) {
        try {
            // Check if device already exists with same IP
            Optional<NetworkConnection> existing = connectionRepository.findByAssignedIpAndIsCurrentlyConnectedTrue(ip);

            if (existing.isEmpty()) {
                NetworkConnection connection = new NetworkConnection();
                connection.setNetwork(network);
                connection.setDeviceName(deviceName);
                connection.setDeviceMac(mac.toUpperCase());
                connection.setAssignedIp(ip);
                connection.setConnectedAt(LocalDateTime.now());
                connection.setConnectionStatus("CONNECTED");
                connection.setIsCurrentlyConnected(true);
                connection.setDataUsageBytes(0L);

                connectionRepository.save(connection);
                log.info("💾 Saved device: {} ({}) at {}", deviceName, mac, ip);
            } else {
                log.debug("Device already exists at IP: {}", ip);
            }

        } catch (Exception e) {
            log.debug("Error saving device {}: {}", ip, e.getMessage());
        }
    }

    // ===========================================
    // EXISTING WIFI SCANNING METHODS (UNCHANGED)
    // ===========================================

    private List<AvailableNetwork> performRealWiFiScan() throws IOException {
        List<AvailableNetwork> networks = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();

        log.info("Performing WiFi scan on OS: {}", os);

        if (os.contains("win")) {
            networks.addAll(scanWindowsWiFiNetworks());
        } else if (os.contains("mac")) {
            networks.addAll(scanMacWiFiNetworks());
        } else if (os.contains("linux")) {
            networks.addAll(scanLinuxWiFiNetworks());
        } else {
            log.warn("Unsupported operating system for WiFi scanning: {}", os);
        }

        return networks;
    }

    private List<AvailableNetwork> scanWindowsWiFiNetworks() throws IOException {
        List<AvailableNetwork> networks = new ArrayList<>();

        try {
            CommandLine cmdLine = CommandLine.parse("netsh wlan show networks mode=bssid");
            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            int exitCode = executor.execute(cmdLine);
            if (exitCode == 0) {
                String output = outputStream.toString("UTF-8");
                networks.addAll(parseWindowsWiFiScanOutput(output));
            } else {
                log.warn("Windows WiFi scan command failed with exit code: {}", exitCode);
            }
        } catch (Exception e) {
            log.error("Error scanning Windows WiFi networks: ", e);
        }

        return networks;
    }

    private List<AvailableNetwork> parseWindowsWiFiScanOutput(String output) {
        List<AvailableNetwork> networks = new ArrayList<>();

        String[] lines = output.split("\\r?\\n");
        String currentSSID = null;
        String currentBSSID = null;
        Integer currentSignal = null;
        String currentSecurity = "Open";
        String currentChannel = null;

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("SSID")) {
                if (currentSSID != null && !currentSSID.isEmpty()) {
                    AvailableNetwork network = createNetworkFromScanData(
                            currentSSID, currentBSSID, currentSignal, currentSecurity, currentChannel);
                    if (network != null) {
                        networks.add(network);
                    }
                }

                currentSSID = line.substring(line.indexOf(":") + 1).trim();
                currentBSSID = null;
                currentSignal = null;
                currentSecurity = "Open";
                currentChannel = null;
            } else if (line.startsWith("Authentication")) {
                currentSecurity = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("Signal")) {
                String signalStr = line.substring(line.indexOf(":") + 1).trim().replace("%", "");
                try {
                    currentSignal = Integer.parseInt(signalStr);
                } catch (NumberFormatException e) {
                    currentSignal = 50;
                }
            } else if (line.startsWith("BSSID")) {
                currentBSSID = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("Channel")) {
                currentChannel = line.substring(line.indexOf(":") + 1).trim();
            }
        }

        if (currentSSID != null && !currentSSID.isEmpty()) {
            AvailableNetwork network = createNetworkFromScanData(
                    currentSSID, currentBSSID, currentSignal, currentSecurity, currentChannel);
            if (network != null) {
                networks.add(network);
            }
        }

        return networks;
    }

    private List<AvailableNetwork> scanMacWiFiNetworks() throws IOException {
        List<AvailableNetwork> networks = new ArrayList<>();

        try {
            CommandLine cmdLine = CommandLine.parse("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -s");
            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            int exitCode = executor.execute(cmdLine);
            if (exitCode == 0) {
                String output = outputStream.toString("UTF-8");
                networks.addAll(parseMacWiFiScanOutput(output));
            } else {
                log.warn("macOS WiFi scan command failed with exit code: {}", exitCode);
            }
        } catch (Exception e) {
            log.error("Error scanning macOS WiFi networks: ", e);
        }

        return networks;
    }

    private List<AvailableNetwork> parseMacWiFiScanOutput(String output) {
        List<AvailableNetwork> networks = new ArrayList<>();

        String[] lines = output.split("\\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                try {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 6) {
                        String ssid = parts[0];
                        String bssid = parts[1];
                        Integer signal = Math.abs(Integer.parseInt(parts[2]));
                        String channel = parts[3];
                        String security = parts.length > 5 ? parts[5] : "NONE";

                        AvailableNetwork network = createNetworkFromScanData(ssid, bssid, signal, security, channel);
                        if (network != null) {
                            networks.add(network);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error parsing macOS WiFi line: {}", line);
                }
            }
        }

        return networks;
    }

    private List<AvailableNetwork> scanLinuxWiFiNetworks() throws IOException {
        List<AvailableNetwork> networks = new ArrayList<>();

        try {
            networks.addAll(scanLinuxWithNmcli());
        } catch (Exception e) {
            log.warn("nmcli scan failed, trying iwlist: ", e);
            try {
                networks.addAll(scanLinuxWithIwlist());
            } catch (Exception e2) {
                log.error("Both nmcli and iwlist failed: ", e2);
            }
        }

        return networks;
    }

    private List<AvailableNetwork> scanLinuxWithNmcli() throws IOException, InterruptedException {
        List<AvailableNetwork> networks = new ArrayList<>();

        try {
            CommandLine scanCmd = CommandLine.parse("nmcli dev wifi rescan");
            DefaultExecutor executor = new DefaultExecutor();
            executor.execute(scanCmd);
            Thread.sleep(2000);
        } catch (Exception e) {
            log.debug("nmcli rescan failed: ", e);
        }

        CommandLine cmdLine = CommandLine.parse("nmcli dev wifi list");
        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);

        int exitCode = executor.execute(cmdLine);
        if (exitCode == 0) {
            String output = outputStream.toString("UTF-8");
            networks.addAll(parseLinuxNmcliOutput(output));
        }

        return networks;
    }

    private List<AvailableNetwork> parseLinuxNmcliOutput(String output) {
        List<AvailableNetwork> networks = new ArrayList<>();

        String[] lines = output.split("\\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                try {
                    String[] parts = line.split("\\s+", 9);
                    if (parts.length >= 8) {
                        String bssid = parts[1];
                        String ssid = parts[2];
                        String channel = parts[4];
                        String signal = parts[6];
                        String security = parts.length > 8 ? parts[8] : "--";

                        if (!ssid.equals("--") && !ssid.isEmpty()) {
                            Integer signalInt = Integer.parseInt(signal);
                            AvailableNetwork network = createNetworkFromScanData(ssid, bssid, signalInt, security, channel);
                            if (network != null) {
                                networks.add(network);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error parsing nmcli line: {}", line);
                }
            }
        }

        return networks;
    }

    private List<AvailableNetwork> scanLinuxWithIwlist() throws IOException {
        List<AvailableNetwork> networks = new ArrayList<>();

        String wifiInterface = findWirelessInterface();
        if (wifiInterface == null) {
            throw new IOException("No wireless interface found");
        }

        CommandLine cmdLine = CommandLine.parse("iwlist " + wifiInterface + " scan");
        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);

        int exitCode = executor.execute(cmdLine);
        if (exitCode == 0) {
            String output = outputStream.toString("UTF-8");
            networks.addAll(parseLinuxIwlistOutput(output));
        }

        return networks;
    }

    private String findWirelessInterface() {
        try {
            CommandLine cmdLine = CommandLine.parse("iwconfig");
            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            executor.execute(cmdLine);
            String output = outputStream.toString("UTF-8");

            String[] lines = output.split("\\n");
            for (String line : lines) {
                if (line.contains("IEEE 802.11")) {
                    return line.split("\\s+")[0];
                }
            }
        } catch (Exception e) {
            log.debug("Error finding wireless interface: ", e);
        }

        String[] commonInterfaces = {"wlan0", "wlp2s0", "wlp3s0", "wifi0"};
        for (String iface : commonInterfaces) {
            try {
                CommandLine testCmd = CommandLine.parse("iwlist " + iface + " scan");
                DefaultExecutor executor = new DefaultExecutor();
                executor.execute(testCmd);
                return iface;
            } catch (Exception e) {
                // Try next interface
            }
        }

        return null;
    }

    private List<AvailableNetwork> parseLinuxIwlistOutput(String output) {
        List<AvailableNetwork> networks = new ArrayList<>();

        String[] cells = output.split("Cell \\d+");

        for (String cell : cells) {
            if (cell.trim().isEmpty()) continue;

            try {
                String ssid = null;
                String bssid = null;
                Integer signal = null;
                String security = "Open";
                String channel = null;

                String[] lines = cell.split("\\n");
                for (String line : lines) {
                    line = line.trim();

                    if (line.contains("Address:")) {
                        bssid = line.substring(line.indexOf("Address:") + 8).trim();
                    } else if (line.contains("ESSID:")) {
                        ssid = line.substring(line.indexOf("ESSID:") + 6).trim().replace("\"", "");
                    } else if (line.contains("Signal level=")) {
                        String signalStr = line.substring(line.indexOf("Signal level=") + 13);
                        signalStr = signalStr.split("\\s+")[0].replace("dBm", "");
                        try {
                            signal = Math.abs(Integer.parseInt(signalStr));
                        } catch (Exception e) {
                            signal = 50;
                        }
                    } else if (line.contains("Encryption key:on")) {
                        security = "WPA/WPA2";
                    } else if (line.contains("Channel:")) {
                        channel = line.substring(line.indexOf("Channel:") + 8).split("\\s+")[0];
                    }
                }

                if (ssid != null && !ssid.isEmpty() && !ssid.equals("\\x00")) {
                    AvailableNetwork network = createNetworkFromScanData(ssid, bssid, signal, security, channel);
                    if (network != null) {
                        networks.add(network);
                    }
                }
            } catch (Exception e) {
                log.debug("Error parsing iwlist cell: ", e);
            }
        }

        return networks;
    }

    private AvailableNetwork createNetworkFromScanData(String ssid, String bssid, Integer signal, String security, String channel) {
        if (ssid == null || ssid.trim().isEmpty() || ssid.equals("--")) {
            return null;
        }

        AvailableNetwork network = new AvailableNetwork();
        network.setSsid(ssid.trim());
        network.setBssid(bssid != null ? bssid.trim() : "00:00:00:00:00:00");
        network.setSignalStrength(signal != null ? signal : 50);
        network.setFrequency(determineFrequency(channel));
        network.setSecurity(security != null ? security : "Open");
        network.setIsSecured(!isOpenNetwork(security));
        network.setNetworkType("WiFi");
        network.setChannel(channel != null ? channel : "Unknown");
        network.setVendor("Unknown");
        network.setIsConnected(false);
        network.setIsAvailable(true);
        network.setLastSeen(LocalDateTime.now());

        return network;
    }

    private String determineFrequency(String channel) {
        if (channel == null) return "2.4GHz";

        try {
            int channelNum = Integer.parseInt(channel);
            if (channelNum >= 36) {
                return "5GHz";
            } else {
                return "2.4GHz";
            }
        } catch (NumberFormatException e) {
            return "2.4GHz";
        }
    }

    private boolean isOpenNetwork(String security) {
        if (security == null) return true;
        security = security.toLowerCase();
        return security.contains("none") || security.contains("open") || security.equals("--") || security.trim().isEmpty();
    }

    // ===========================================
    // CONNECTION METHODS
    // ===========================================

    private boolean attemptWiFiConnection(String ssid, String password, boolean isSecured) {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                return connectWindowsWiFi(ssid, password, isSecured);
            } else if (os.contains("mac")) {
                return connectMacWiFi(ssid, password, isSecured);
            } else if (os.contains("linux")) {
                return connectLinuxWiFi(ssid, password, isSecured);
            }

            return false;
        } catch (Exception e) {
            log.error("Error connecting to WiFi: ", e);
            return false;
        }
    }

    private boolean connectWindowsWiFi(String ssid, String password, boolean isSecured) throws IOException {
        CommandLine connectCmd = CommandLine.parse("netsh wlan connect name=\"" + ssid + "\"");
        DefaultExecutor executor = new DefaultExecutor();
        int exitCode = executor.execute(connectCmd);
        return exitCode == 0;
    }

    private boolean connectMacWiFi(String ssid, String password, boolean isSecured) throws IOException {
        if (isSecured) {
            CommandLine cmdLine = CommandLine.parse("networksetup -setairportnetwork en0 \"" + ssid + "\" \"" + password + "\"");
            DefaultExecutor executor = new DefaultExecutor();
            int exitCode = executor.execute(cmdLine);
            return exitCode == 0;
        } else {
            CommandLine cmdLine = CommandLine.parse("networksetup -setairportnetwork en0 \"" + ssid + "\"");
            DefaultExecutor executor = new DefaultExecutor();
            int exitCode = executor.execute(cmdLine);
            return exitCode == 0;
        }
    }

    private boolean connectLinuxWiFi(String ssid, String password, boolean isSecured) throws IOException {
        if (isSecured) {
            CommandLine cmdLine = CommandLine.parse("nmcli dev wifi connect \"" + ssid + "\" password \"" + password + "\"");
            DefaultExecutor executor = new DefaultExecutor();
            int exitCode = executor.execute(cmdLine);
            return exitCode == 0;
        } else {
            CommandLine cmdLine = CommandLine.parse("nmcli dev wifi connect \"" + ssid + "\"");
            DefaultExecutor executor = new DefaultExecutor();
            int exitCode = executor.execute(cmdLine);
            return exitCode == 0;
        }
    }

    private boolean disconnectFromCurrentNetwork() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            CommandLine cmdLine;

            if (os.contains("win")) {
                cmdLine = CommandLine.parse("netsh wlan disconnect");
            } else if (os.contains("mac")) {
                cmdLine = CommandLine.parse("networksetup -setairportpower en0 off");
                DefaultExecutor executor = new DefaultExecutor();
                executor.execute(cmdLine);
                Thread.sleep(1000);
                cmdLine = CommandLine.parse("networksetup -setairportpower en0 on");
            } else if (os.contains("linux")) {
                String wifiInterface = findWirelessInterface();
                if (wifiInterface != null) {
                    cmdLine = CommandLine.parse("nmcli dev disconnect " + wifiInterface);
                } else {
                    return false;
                }
            } else {
                return false;
            }

            DefaultExecutor executor = new DefaultExecutor();
            int exitCode = executor.execute(cmdLine);
            return exitCode == 0;

        } catch (Exception e) {
            log.error("Error disconnecting from network: ", e);
            return false;
        }
    }

    private String getCurrentlyConnectedNetwork() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            CommandLine cmdLine;

            if (os.contains("win")) {
                cmdLine = CommandLine.parse("netsh wlan show interfaces");
            } else if (os.contains("mac")) {
                cmdLine = CommandLine.parse("networksetup -getairportnetwork en0");
            } else if (os.contains("linux")) {
                cmdLine = CommandLine.parse("iwgetid -r");
            } else {
                return "Not Connected";
            }

            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            int exitCode = executor.execute(cmdLine);
            if (exitCode != 0) {
                return "Not Connected";
            }

            String output = outputStream.toString("UTF-8").trim();

            if (os.contains("win")) {
                Pattern pattern = Pattern.compile("SSID\\s*:\\s*(.+)");
                Matcher matcher = pattern.matcher(output);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            } else if (os.contains("mac")) {
                return output.replace("Current Wi-Fi Network: ", "").trim();
            } else if (os.contains("linux")) {
                return output.isEmpty() ? "Not Connected" : output;
            }

        } catch (Exception e) {
            log.debug("Error getting connected network: ", e);
        }

        return "Not Connected";
    }

    private String getCurrentIpAddress() {
        try {
            CommandLine cmdLine;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                cmdLine = CommandLine.parse("ipconfig");
            } else {
                cmdLine = CommandLine.parse("ifconfig");
            }

            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            executor.execute(cmdLine);
            String output = outputStream.toString("UTF-8");

            Pattern pattern = Pattern.compile("192\\.168\\.\\d+\\.\\d+|10\\.\\d+\\.\\d+\\.\\d+|172\\.\\d+\\.\\d+\\.\\d+");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                return matcher.group();
            }

        } catch (Exception e) {
            log.debug("Error getting IP address: ", e);
        }

        return "192.168.1.100";
    }

    private String getCurrentDeviceMac() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            CommandLine cmdLine;

            if (os.contains("win")) {
                cmdLine = CommandLine.parse("getmac");
            } else if (os.contains("mac")) {
                cmdLine = CommandLine.parse("ifconfig en0");
            } else {
                String wifiInterface = findWirelessInterface();
                if (wifiInterface != null) {
                    cmdLine = CommandLine.parse("cat /sys/class/net/" + wifiInterface + "/address");
                } else {
                    return "00:00:00:00:00:00";
                }
            }

            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            executor.execute(cmdLine);
            String output = outputStream.toString("UTF-8");

            Pattern pattern = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                return matcher.group();
            }

        } catch (Exception e) {
            log.debug("Error getting device MAC: ", e);
        }

        return "00:00:00:00:00:00";
    }

    // ===========================================
    // DATABASE HELPER METHODS
    // ===========================================

    public void markOldNetworksAsUnavailable() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);
        List<AvailableNetwork> oldNetworks = networkRepository.findAll().stream()
                .filter(n -> n.getLastSeen().isBefore(cutoff))
                .collect(Collectors.toList());

        for (AvailableNetwork network : oldNetworks) {
            network.setIsAvailable(false);
            networkRepository.save(network);
        }
    }

    @Transactional
    public void saveOrUpdateScannedNetwork(AvailableNetwork network) {
        Optional<AvailableNetwork> existingOpt = networkRepository.findBySsid(network.getSsid());

        if (existingOpt.isPresent()) {
            AvailableNetwork existing = existingOpt.get();
            existing.setSignalStrength(network.getSignalStrength());
            existing.setSecurity(network.getSecurity());
            existing.setIsSecured(network.getIsSecured());
            existing.setChannel(network.getChannel());
            existing.setFrequency(network.getFrequency());
            existing.setLastSeen(LocalDateTime.now());
            existing.setIsAvailable(true);
            networkRepository.save(existing);
        } else {
            networkRepository.save(network);
        }
    }

    @Transactional(readOnly = true)
    protected void updateConnectionStatuses() {
        List<NetworkConnection> activeConnections = connectionRepository.findByIsCurrentlyConnectedTrue();
        for (NetworkConnection connection : activeConnections) {
            long minutes = java.time.Duration.between(connection.getConnectedAt(), LocalDateTime.now()).toMinutes();
            connection.setConnectionDurationMinutes((int) minutes);
            connectionRepository.save(connection);
        }
    }

    // ===========================================
    // STATISTICS HELPER METHODS
    // ===========================================

    @Transactional(readOnly = true)
    protected NetworkStatsDTO calculateNetworkStats(List<AvailableNetwork> networks, List<NetworkConnection> connections) {
        NetworkStatsDTO stats = new NetworkStatsDTO();

        Long totalDataBytes = connectionRepository.getTotalDataUsageSince(LocalDateTime.now().minusDays(1));
        stats.setTotalDataUsageGB(totalDataBytes != null ? totalDataBytes / (1024.0 * 1024.0 * 1024.0) : 0.0);

        stats.setTotalConnectedDevices(connections.size());

        Double avgSignal = networks.stream()
                .filter(AvailableNetwork::getIsAvailable)
                .mapToDouble(AvailableNetwork::getSignalStrength)
                .average()
                .orElse(0.0);
        stats.setAverageSignalStrength(avgSignal);

        long fiveGhzCount = networks.stream().filter(n -> "5GHz".equals(n.getFrequency())).count();
        long twoGhzCount = networks.size() - fiveGhzCount;
        stats.setPrimaryFrequency(fiveGhzCount > twoGhzCount ? "5GHz" : "2.4GHz");

        long securedCount = networks.stream().filter(AvailableNetwork::getIsSecured).count();
        long openCount = networks.size() - securedCount;
        stats.setSecuredNetworksCount((int) securedCount);
        stats.setOpenNetworksCount((int) openCount);

        return stats;
    }

    @Transactional(readOnly = true)
    protected String calculateTotalTimeUsed(List<NetworkConnection> connections) {
        long totalMinutes = connections.stream()
                .mapToLong(conn -> conn.getConnectionDurationMinutes() != null ? conn.getConnectionDurationMinutes() : 0)
                .sum();

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        return String.format("%d.%dh", hours, minutes);
    }

    @Transactional(readOnly = true)
    protected Integer getConnectedDeviceCount() {
        Long count = connectionRepository.countCurrentConnections();
        return count != null ? count.intValue() : 0;
    }

    private Boolean isVpnActive() {
        return false;
    }

    private Integer calculateDailyVisitedSites() {
        return 150 + (int)(Math.random() * 100);
    }

    private AvailableNetworkDTO convertToDTO(AvailableNetwork network) {
        return new AvailableNetworkDTO(
                network.getId(),
                network.getSsid(),
                network.getBssid(),
                network.getSignalStrength(),
                network.getFrequency(),
                network.getSecurity(),
                network.getIsSecured(),
                network.getNetworkType(),
                network.getChannel(),
                network.getVendor(),
                network.getIsConnected(),
                network.getIsAvailable(),
                network.getLastSeen(),
                network.getLocation()
        );
    }

    @Transactional(readOnly = true)
    protected ConnectedDeviceDTO convertToConnectedDeviceDTO(NetworkConnection connection) {
        return new ConnectedDeviceDTO(
                connection.getDeviceName(),
                connection.getDeviceMac(),
                connection.getAssignedIp(),
                connection.getConnectedAt(),
                connection.getDataUsageBytes(),
                connection.getConnectionStatus(),
                connection.getNetwork().getSignalStrength()
        );
    }
}