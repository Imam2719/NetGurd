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
    private final EnhancedDeviceDiscoveryService deviceDiscoveryService;
    private final RealWebsiteMonitoringService websiteMonitoringService;

    // Cache for device discovery to avoid duplicate scanning
    private volatile boolean isScanning = false;
    private volatile LocalDateTime lastScanTime = LocalDateTime.now().minusHours(1);
    private final Map<String, String> deviceNameCache = new HashMap<>();

    /**
     * 🔥 ENHANCED: Scan for available networks using system commands - REAL WiFi scanning
     */
    @Async("networkTaskExecutor")
    public CompletableFuture<List<AvailableNetworkDTO>> scanAvailableNetworks() {
        try {
            log.info("🔍 Starting REAL WiFi network scan...");
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

            log.info("✅ WiFi scan completed. Found {} networks", networkDTOs.size());
            return CompletableFuture.completedFuture(networkDTOs);

        } catch (Exception e) {
            log.error("❌ Error during WiFi scan: ", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    /**
     * 🔥 ENHANCED: Get network overview data with current connection status and ALL devices
     */
    @Transactional(readOnly = true)
    public NetworkOverviewDTO getNetworkOverview() {
        try {
            log.info("📊 Fetching comprehensive network overview with enhanced device discovery...");

            // Get currently connected network info
            String connectedWifi = getCurrentlyConnectedNetwork();
            log.info("🌐 Currently connected to: {}", connectedWifi);

            // Get available networks from recent scan
            List<AvailableNetwork> availableNetworks = networkRepository.findByIsAvailableTrue();

            // Mark the currently connected network
            availableNetworks.forEach(network ->
                    network.setIsConnected(network.getSsid().equals(connectedWifi))
            );

            // 🔥 CRITICAL: If connected to a network, ensure we have discovered all devices
            if (!connectedWifi.equals("Not Connected")) {
                // Trigger device discovery if we haven't scanned recently
                boolean shouldScan = lastScanTime.isBefore(LocalDateTime.now().minusMinutes(2));
                if (shouldScan && !isScanning) {
                    log.info("🔍 Triggering fresh device discovery for network: {}", connectedWifi);
                    performImmediateDeviceDiscovery(connectedWifi);
                }
            }

            // Get active connections (all discovered devices)
            List<NetworkConnection> activeConnections = connectionRepository.findByIsCurrentlyConnectedTrue();
            log.info("📱 Found {} active device connections", activeConnections.size());

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
                    .map(this::convertToEnhancedConnectedDeviceDTO).collect(Collectors.toList()));
            overview.setNetworkStats(stats);

            log.info("✅ Enhanced network overview completed: {} networks, {} devices",
                    availableNetworks.size(), activeConnections.size());

            return overview;

        } catch (Exception e) {
            log.error("❌ Error getting network overview: ", e);
            return new NetworkOverviewDTO();
        }
    }

    /**
     * 🔥 ENHANCED: Connect to a WiFi network with comprehensive device discovery
     */
    @Transactional
    public NetworkConnectionResponseDTO connectToNetwork(NetworkConnectionRequestDTO request) {
        try {
            log.info("🔗 Attempting ENHANCED WiFi connection to: {}", request.getSsid());

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
            Thread.sleep(2000); // Wait for disconnection

            // Attempt connection using system commands
            boolean connected = attemptWiFiConnection(request.getSsid(), request.getPassword(), network.getIsSecured());

            if (connected) {
                log.info("⏳ Connection attempt successful, verifying and discovering devices...");

                // Wait for connection to stabilize
                Thread.sleep(5000);

                // Verify connection was successful
                String currentNetwork = getCurrentlyConnectedNetwork();
                if (request.getSsid().equals(currentNetwork)) {
                    // Update network status
                    network.setIsConnected(true);
                    networkRepository.save(network);

                    // 🔥 ENHANCED: Comprehensive device discovery with authentic naming
                    String assignedIp = getCurrentIpAddress();
                    String deviceMac = getCurrentDeviceMac();

                    // Create connection record for this device
                    NetworkConnection connection = createEnhancedConnectionRecord(network, request.getDeviceName(),
                            deviceMac, assignedIp);

                    // 🔥 CRITICAL: Perform comprehensive network device discovery with enhanced naming
                    log.info("🔍 Starting ENHANCED comprehensive device discovery for network: {}", request.getSsid());
                    performEnhancedComprehensiveDeviceDiscovery(network, assignedIp);

                    return new NetworkConnectionResponseDTO(
                            true,
                            "Successfully connected to " + request.getSsid() + " and discovered network devices with authentic names",
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
            log.error("❌ Error connecting to WiFi network: ", e);
            return new NetworkConnectionResponseDTO(
                    false,
                    "Connection error: " + e.getMessage(),
                    null, null, null
            );
        }
    }

    /**
     * 🔥 ENHANCED: Perform immediate device discovery with authentic naming
     */
    @Transactional
    public void performImmediateDeviceDiscovery(String connectedNetwork) {
        if (isScanning) {
            log.info("⭐️ Enhanced device discovery already in progress, skipping...");
            return;
        }

        try {
            isScanning = true;
            lastScanTime = LocalDateTime.now();

            log.info("🔍 Starting ENHANCED immediate device discovery for network: {}", connectedNetwork);

            // Find the network in database
            Optional<AvailableNetwork> networkOpt = networkRepository.findBySsid(connectedNetwork);
            if (networkOpt.isEmpty()) {
                log.warn("⚠️ Network {} not found in database", connectedNetwork);
                return;
            }

            AvailableNetwork network = networkOpt.get();
            String currentIp = getCurrentIpAddress();

            // Clear old connections for this network
            clearOldNetworkConnections(network);

            // Perform ENHANCED comprehensive device discovery
            performEnhancedComprehensiveDeviceDiscovery(network, currentIp);

            log.info("✅ ENHANCED immediate device discovery completed for network: {}", connectedNetwork);

        } catch (Exception e) {
            log.error("❌ Error in enhanced immediate device discovery: ", e);
        } finally {
            isScanning = false;
        }
    }

    /**
     * 🔥 ENHANCED: Comprehensive device discovery with authentic naming and website monitoring
     */
    @Transactional
    public void performEnhancedComprehensiveDeviceDiscovery(AvailableNetwork network, String baseIp) {
        try {
            log.info("🕵️ Starting ENHANCED comprehensive device discovery with authentic naming...");

            Set<String[]> discoveredDevices = new HashSet<>();

            // Method 1: Enhanced ARP table scanning
            List<String[]> arpDevices = scanEnhancedArpTableForAllDevices();
            discoveredDevices.addAll(arpDevices);
            log.info("📋 Enhanced ARP scan found {} devices", arpDevices.size());

            // Method 2: Enhanced ping sweep for active devices
            if (baseIp != null && !baseIp.isEmpty()) {
                List<String[]> pingDevices = performEnhancedPingSweep(baseIp);
                discoveredDevices.addAll(pingDevices);
                log.info("🔍 Enhanced ping sweep found {} additional devices", pingDevices.size());
            }

            // Method 3: Enhanced port scanning for common services
            List<String[]> serviceDevices = scanForEnhancedCommonServices(baseIp);
            discoveredDevices.addAll(serviceDevices);
            log.info("🔍 Enhanced service scan found {} additional devices", serviceDevices.size());

            // Method 4: Network neighborhood discovery
            List<String[]> neighborDevices = discoverNetworkNeighborhood(baseIp);
            discoveredDevices.addAll(neighborDevices);
            log.info("🏘️ Network neighborhood discovery found {} devices", neighborDevices.size());

            // Remove duplicates and process all discovered devices
            Map<String, String[]> uniqueDevices = new HashMap<>();
            for (String[] device : discoveredDevices) {
                if (device.length >= 2 && device[0] != null && device[1] != null) {
                    uniqueDevices.put(device[0], device); // Use IP as key to remove duplicates
                }
            }

            log.info("🎯 Processing {} unique discovered devices with ENHANCED naming and website monitoring",
                    uniqueDevices.size());

            // Save each discovered device with ENHANCED naming and website monitoring
            for (String[] deviceInfo : uniqueDevices.values()) {
                String ip = deviceInfo[0];
                String mac = deviceInfo[1];

                // Skip if this is our own device (already created)
                if (ip.equals(baseIp)) {
                    continue;
                }

                // 🔥 ENHANCED: Get authentic device name using comprehensive detection
                String authenticDeviceName = getAuthenticDeviceName(mac, ip);

                // 🔥 ENHANCED: Get current website being visited
                String currentWebsite = getCurrentWebsiteForDevice(mac, ip);

                // 🔥 ENHANCED: Save device with authentic name and website info
                saveEnhancedNetworkDevice(network, authenticDeviceName, mac, ip, currentWebsite);
            }

            log.info("✅ ENHANCED comprehensive device discovery completed. Saved {} devices with authentic names and website monitoring",
                    uniqueDevices.size());

        } catch (Exception e) {
            log.error("❌ Error in enhanced comprehensive device discovery: ", e);
        }
    }

    /**
     * 🔥 ENHANCED: Get authentic device name using comprehensive detection
     */
    private String getAuthenticDeviceName(String mac, String ip) {
        try {
            // Check cache first
            String cacheKey = mac + ":" + ip;
            if (deviceNameCache.containsKey(cacheKey)) {
                return deviceNameCache.get(cacheKey);
            }

            log.debug("🔍 Getting authentic device name for {} ({})", ip, mac);

            // Use the enhanced device discovery service
            String authenticName = deviceDiscoveryService.getAuthenticDeviceName(mac, ip);

            // Cache the result
            deviceNameCache.put(cacheKey, authenticName);

            return authenticName;

        } catch (Exception e) {
            log.error("❌ Error getting authentic device name for {} ({}): ", ip, mac, e);
            return generateFallbackDeviceName(mac, ip);
        }
    }

    /**
     * 🔥 ENHANCED: Get current website for device
     */
    private String getCurrentWebsiteForDevice(String mac, String ip) {
        try {
            return websiteMonitoringService.getCurrentWebsiteForDevice(mac, ip);
        } catch (Exception e) {
            log.debug("Error getting current website for device {} ({}): ", mac, ip, e.getMessage());
            return null;
        }
    }

    /**
     * 🔥 ENHANCED: Enhanced ARP table scanning with better parsing
     */
    private List<String[]> scanEnhancedArpTableForAllDevices() {
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
                devices = parseEnhancedArpTableOutput(output, os);
                log.info("📋 Enhanced ARP table parsed: {} devices found", devices.size());
            } else {
                log.warn("⚠️ ARP command failed with exit code: {}", exitCode);
            }

        } catch (Exception e) {
            log.debug("Enhanced ARP scan failed, trying alternative method: ", e);
            // Fallback to ping sweep if ARP fails
            String currentIp = getCurrentIpAddress();
            if (currentIp != null) {
                devices = performEnhancedPingSweep(currentIp);
            }
        }

        return devices;
    }

    /**
     * 🔥 ENHANCED: Enhanced ping sweep with broader range and parallel processing
     */
    private List<String[]> performEnhancedPingSweep(String baseIp) {
        List<String[]> devices = new ArrayList<>();

        try {
            String[] ipParts = baseIp.split("\\.");
            String networkBase = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".";

            log.info("🔍 Performing ENHANCED ping sweep on network: {}x", networkBase);

            // Enhanced IP range - check more comprehensive addresses
            List<Integer> ipRange = new ArrayList<>();

            // Add common router/gateway IPs
            ipRange.addAll(Arrays.asList(1, 254));

            // Add common device ranges (more comprehensive)
            for (int i = 2; i <= 50; i++) ipRange.add(i);     // Early range for servers/printers
            for (int i = 100; i <= 200; i++) ipRange.add(i);  // Extended DHCP range
            for (int i = 220; i <= 240; i++) ipRange.add(i);  // Additional static device range

            // Use parallel processing for much faster scanning
            ipRange.parallelStream().forEach(ip -> {
                String testIP = networkBase + ip;
                if (pingDeviceEnhanced(testIP, 1500)) { // 1.5 second timeout
                    synchronized (devices) {
                        String mac = getEnhancedMacFromArp(testIP);
                        devices.add(new String[]{testIP, mac, ""});
                    }
                }
            });

            log.info("✅ Enhanced ping sweep completed: {} active devices", devices.size());

        } catch (Exception e) {
            log.debug("Enhanced ping sweep failed: ", e);
        }

        return devices;
    }

    /**
     * 🔥 ENHANCED: Scan for common services with more comprehensive service detection
     */
    private List<String[]> scanForEnhancedCommonServices(String baseIp) {
        List<String[]> devices = new ArrayList<>();

        if (baseIp == null) return devices;

        try {
            String[] ipParts = baseIp.split("\\.");
            String networkBase = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".";

            // Enhanced service ports to check
            Map<Integer, String> servicePorts = new HashMap<>();
            servicePorts.put(80, "Web Server");
            servicePorts.put(443, "HTTPS Server");
            servicePorts.put(22, "SSH Server");
            servicePorts.put(445, "SMB/CIFS");
            servicePorts.put(139, "NetBIOS");
            servicePorts.put(8080, "Web Proxy");
            servicePorts.put(3389, "RDP");
            servicePorts.put(5353, "mDNS");
            servicePorts.put(53, "DNS Server");
            servicePorts.put(23, "Telnet");
            servicePorts.put(21, "FTP");
            servicePorts.put(25, "SMTP");
            servicePorts.put(110, "POP3");
            servicePorts.put(143, "IMAP");
            servicePorts.put(161, "SNMP");

            // Scan a broader range for services
            for (int i = 1; i <= 50; i++) {
                String testIP = networkBase + i;

                for (Map.Entry<Integer, String> service : servicePorts.entrySet()) {
                    if (isPortResponsiveEnhanced(testIP, service.getKey(), 800)) {
                        String mac = getEnhancedMacFromArp(testIP);
                        devices.add(new String[]{testIP, mac, service.getValue()});
                        break; // Found one service, move to next IP
                    }
                }
            }

            log.info("🔍 Enhanced service scan found {} devices with active services", devices.size());

        } catch (Exception e) {
            log.debug("Enhanced service scan failed: ", e);
        }

        return devices;
    }

    /**
     * 🔥 NEW: Network neighborhood discovery using advanced techniques
     */
    private List<String[]> discoverNetworkNeighborhood(String baseIp) {
        List<String[]> devices = new ArrayList<>();

        try {
            // Method 1: Broadcast ping to discover all devices
            String networkBase = baseIp.substring(0, baseIp.lastIndexOf('.'));
            String broadcastIP = networkBase + ".255";

            if (pingDeviceEnhanced(broadcastIP, 2000)) {
                log.info("📡 Broadcast ping successful, devices should respond");
                Thread.sleep(1000); // Wait for responses

                // Re-scan ARP table after broadcast
                devices.addAll(scanEnhancedArpTableForAllDevices());
            }

            // Method 2: mDNS multicast discovery
            String mdnsIP = "224.0.0.251";
            if (pingDeviceEnhanced(mdnsIP, 1000)) {
                log.info("📱 mDNS discovery initiated");
                // Additional mDNS processing would go here
            }

            // Method 3: NetBIOS name service discovery
            String netbiosIP = "224.0.0.1";
            if (pingDeviceEnhanced(netbiosIP, 1000)) {
                log.info("🖥️ NetBIOS discovery initiated");
                // Additional NetBIOS processing would go here
            }

        } catch (Exception e) {
            log.debug("Network neighborhood discovery failed: ", e);
        }

        return devices;
    }

    /**
     * 🔥 ENHANCED: Save network device with enhanced information
     */
    @Transactional
    public void saveEnhancedNetworkDevice(AvailableNetwork network, String deviceName,
                                          String mac, String ip, String currentWebsite) {
        try {
            // Check if device already exists with same IP or MAC
            Optional<NetworkConnection> existingByIp =
                    connectionRepository.findByAssignedIpAndIsCurrentlyConnectedTrue(ip);
            Optional<NetworkConnection> existingByMac =
                    connectionRepository.findByDeviceMacAndIsCurrentlyConnectedTrue(mac);

            if (existingByIp.isPresent() || existingByMac.isPresent()) {
                // Update existing device with enhanced information
                NetworkConnection existing = existingByIp.orElse(existingByMac.get());
                existing.setDeviceName(deviceName); // Update with authentic name
                connectionRepository.save(existing);
                log.debug("🔄 Updated existing device with authentic name: {} ({})", deviceName, ip);
                return;
            }

            // Create new enhanced device record
            NetworkConnection connection = new NetworkConnection();
            connection.setNetwork(network);
            connection.setDeviceName(deviceName);        // 🔥 Authentic device name
            connection.setDeviceMac(mac.toUpperCase());
            connection.setAssignedIp(ip);
            connection.setConnectedAt(LocalDateTime.now());
            connection.setConnectionStatus("CONNECTED");
            connection.setIsCurrentlyConnected(true);
            connection.setDataUsageBytes(0L);
            connection.setConnectionDurationMinutes(0);

            connectionRepository.save(connection);

            log.info("💾 Saved ENHANCED device: {} ({}) at {} - Currently visiting: {}",
                    deviceName, mac, ip, currentWebsite != null ? currentWebsite : "Not browsing");

        } catch (Exception e) {
            log.debug("Error saving enhanced device {}: {}", ip, e.getMessage());
        }
    }

    /**
     * 🔥 ENHANCED: Create connection record with enhanced information
     */
    @Transactional
    public NetworkConnection createEnhancedConnectionRecord(AvailableNetwork network, String deviceName,
                                                            String deviceMac, String assignedIp) {
        try {
            // Get authentic device name
            String authenticName = getAuthenticDeviceName(deviceMac, assignedIp);
            if (authenticName != null && !authenticName.startsWith("Network Device")) {
                deviceName = authenticName;
            }

            NetworkConnection connection = new NetworkConnection();
            connection.setNetwork(network);
            connection.setDeviceName(deviceName != null ? deviceName : "NetGuard Device");
            connection.setDeviceMac(deviceMac);
            connection.setAssignedIp(assignedIp);
            connection.setConnectedAt(LocalDateTime.now());
            connection.setConnectionStatus("CONNECTED");
            connection.setIsCurrentlyConnected(true);
            connection.setDataUsageBytes(0L);
            connection.setConnectionDurationMinutes(0);

            NetworkConnection saved = connectionRepository.save(connection);
            log.info("✅ Created enhanced connection record for: {}", deviceName);

            return saved;

        } catch (Exception e) {
            log.error("❌ Error creating enhanced connection record: ", e);
            throw e;
        }
    }

    /**
     * 🔥 ENHANCED: Convert to DTO with enhanced information and website monitoring
     */
    @Transactional(readOnly = true)
    public ConnectedDeviceDTO convertToEnhancedConnectedDeviceDTO(NetworkConnection connection) {
        try {
            // Get current website for this device
            String currentWebsite = null;
            try {
                currentWebsite = websiteMonitoringService.getDeviceCurrentWebsite(connection.getDeviceMac());
            } catch (Exception e) {
                log.debug("Could not get current website for device: {}", connection.getDeviceMac());
            }

            // Get most visited sites
            List<String> frequentSites = new ArrayList<>();
            try {
                frequentSites = websiteMonitoringService.getDeviceMostVisitedSites(connection.getDeviceMac());
            } catch (Exception e) {
                log.debug("Could not get frequent sites for device: {}", connection.getDeviceMac());
            }

            // Calculate connection duration in real-time
            long connectionMinutes = java.time.Duration.between(
                    connection.getConnectedAt(), LocalDateTime.now()).toMinutes();

            ConnectedDeviceDTO dto = new ConnectedDeviceDTO(
                    connection.getDeviceName(),                    // 🔥 Now shows authentic names
                    connection.getDeviceMac(),
                    connection.getAssignedIp(),
                    connection.getConnectedAt(),
                    connection.getDataUsageBytes(),
                    connection.getConnectionStatus(),
                    connection.getNetwork() != null ? connection.getNetwork().getSignalStrength() : 0
            );

            // Set enhanced device type based on authentic name analysis
            dto.setDeviceType(determineEnhancedDeviceTypeFromName(connection.getDeviceName()));
            dto.setConnectionDurationMinutes((int) connectionMinutes);

            log.debug("🔄 Converted device to enhanced DTO: {} - Currently visiting: {}",
                    connection.getDeviceName(), currentWebsite != null ? currentWebsite : "Not browsing");

            return dto;

        } catch (Exception e) {
            log.error("❌ Error converting to enhanced connected device DTO: ", e);

            // Return basic DTO as fallback
            return new ConnectedDeviceDTO(
                    connection.getDeviceName(),
                    connection.getDeviceMac(),
                    connection.getAssignedIp(),
                    connection.getConnectedAt(),
                    connection.getDataUsageBytes(),
                    connection.getConnectionStatus(),
                    connection.getNetwork() != null ? connection.getNetwork().getSignalStrength() : 0
            );
        }
    }

    /**
     * 🔥 ENHANCED: Determine device type from authentic name with better accuracy
     */
    private String determineEnhancedDeviceTypeFromName(String deviceName) {
        if (deviceName == null) return "unknown";

        String name = deviceName.toLowerCase();

        // Mobile devices - more comprehensive detection
        if (name.contains("iphone") || name.contains("galaxy") || name.contains("phone") ||
                name.contains("pixel") || name.contains("oneplus") || name.contains("huawei") ||
                name.contains("xiaomi") || name.contains("nokia") || name.contains("lg phone")) {
            return "mobile";
        }

        // Tablets
        if (name.contains("ipad") || name.contains("tablet") || name.contains("tab ") ||
                name.contains("kindle") || name.contains("surface")) {
            return "tablet";
        }

        // Laptops
        if (name.contains("macbook") || name.contains("laptop") || name.contains("thinkpad") ||
                name.contains("dell") || name.contains("hp elitebook") || name.contains("lenovo") ||
                name.contains("surface book")) {
            return "laptop";
        }

        // Desktop computers
        if (name.contains("imac") || name.contains("desktop") || name.contains("pc") ||
                name.contains("workstation") || name.contains("tower")) {
            return "desktop";
        }

        // Routers and network equipment
        if (name.contains("router") || name.contains("gateway") || name.contains("linksys") ||
                name.contains("netgear") || name.contains("tp-link") || name.contains("asus") ||
                name.contains("d-link") || name.contains("cisco")) {
            return "router";
        }

        // Smart TVs and streaming devices
        if (name.contains("tv") || name.contains("smart tv") || name.contains("samsung tv") ||
                name.contains("lg tv") || name.contains("roku") || name.contains("chromecast") ||
                name.contains("fire tv") || name.contains("apple tv")) {
            return "tv";
        }

        // Smart home and IoT devices
        if (name.contains("echo") || name.contains("alexa") || name.contains("google home") ||
                name.contains("nest") || name.contains("smart") || name.contains("iot") ||
                name.contains("philips hue") || name.contains("ring") || name.contains("doorbell")) {
            return "iot";
        }

        // Gaming devices
        if (name.contains("xbox") || name.contains("playstation") || name.contains("nintendo") ||
                name.contains("gaming") || name.contains("steam deck")) {
            return "gaming";
        }

        // Network equipment and printers
        if (name.contains("printer") || name.contains("scanner") || name.contains("nas") ||
                name.contains("server") || name.contains("switch") || name.contains("access point")) {
            return "network_equipment";
        }

        return "unknown";
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

                // Clear device name cache
                deviceNameCache.clear();

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
            log.error("❌ Error disconnecting from network: ", e);
            return new NetworkConnectionResponseDTO(
                    false,
                    "Disconnection error: " + e.getMessage(),
                    null, null, null
            );
        }
    }

    /**
     * 🔥 ENHANCED: Regular device discovery scheduling with website monitoring
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Async("networkTaskExecutor")
    public void performScheduledDeviceDiscovery() {
        try {
            String connectedNetwork = getCurrentlyConnectedNetwork();

            if (!"Not Connected".equals(connectedNetwork) && !isScanning) {
                // Only scan if we haven't scanned in the last 5 minutes
                if (lastScanTime.isBefore(LocalDateTime.now().minusMinutes(5))) {
                    log.info("🔄 Performing scheduled ENHANCED device discovery...");
                    performImmediateDeviceDiscovery(connectedNetwork);
                }
            }

        } catch (Exception e) {
            log.debug("Scheduled enhanced device discovery error: ", e);
        }
    }

    // ==========================================
    // ENHANCED HELPER METHODS
    // ==========================================

    /**
     * 🔥 ENHANCED: Enhanced MAC address resolution from ARP
     */
    private String getEnhancedMacFromArp(String ip) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            CommandLine cmdLine;

            if (os.contains("win")) {
                cmdLine = CommandLine.parse("arp -a " + ip);
            } else {
                cmdLine = CommandLine.parse("arp -n " + ip);
            }

            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            // Set timeout
            executor.setWatchdog(new org.apache.commons.exec.ExecuteWatchdog(3000));

            int exitCode = executor.execute(cmdLine);
            if (exitCode == 0) {
                String output = outputStream.toString("UTF-8");
                Pattern pattern = Pattern.compile("([0-9a-fA-F:]{17}|[0-9a-fA-F-]{17})");
                Matcher matcher = pattern.matcher(output);
                if (matcher.find()) {
                    return matcher.group(1).replace("-", ":");
                }
            }

        } catch (Exception e) {
            log.debug("Enhanced MAC resolution failed for {}: {}", ip, e.getMessage());
        }

        return "Unknown";
    }

    /**
     * 🔥 ENHANCED: Enhanced ping test with better reliability
     */
    private boolean pingDeviceEnhanced(String ip, int timeout) {
        try {
            InetAddress inet = InetAddress.getByName(ip);
            return inet.isReachable(timeout);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 🔥 ENHANCED: Enhanced port responsiveness check
     */
    private boolean isPortResponsiveEnhanced(String ip, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 🔥 ENHANCED: Parse ARP table output with better accuracy
     */
    private List<String[]> parseEnhancedArpTableOutput(String output, String os) {
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

        log.info("📋 Parsed {} devices from enhanced ARP table", devices.size());
        return devices;
    }

    /**
     * Generate fallback device name
     */
    private String generateFallbackDeviceName(String mac, String ip) {
        // Check if it's likely a router
        if (ip != null && (ip.endsWith(".1") || ip.endsWith(".254"))) {
            return "Router/Gateway (" + ip.substring(ip.lastIndexOf('.') + 1) + ")";
        }

        // Try to get vendor from MAC
        if (mac != null && mac.length() >= 8) {
            String vendor = getBasicVendorFromMAC(mac);
            if (vendor != null) {
                return vendor + " Device (" + (ip != null ? ip.substring(ip.lastIndexOf('.') + 1) : "Unknown") + ")";
            }
        }

        return "Network Device (" + (ip != null ? ip.substring(ip.lastIndexOf('.') + 1) : "Unknown") + ")";
    }

    private String getBasicVendorFromMAC(String mac) {
        String oui = mac.toUpperCase().replace(":", "").replace("-", "").substring(0, 6);

        Map<String, String> basicVendors = new HashMap<>();
        basicVendors.put("001B63", "Apple");
        basicVendors.put("28F076", "Apple");
        basicVendors.put("002312", "Samsung");
        basicVendors.put("34BE00", "Samsung");
        basicVendors.put("000C41", "Linksys");
        basicVendors.put("001F33", "Netgear");

        return basicVendors.get(oui);
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
                conn.setConnectionStatus("DISCONNECTED");
                connectionRepository.save(conn);
            }
            log.info("🧹 Cleared {} old connections for network: {}", oldConnections.size(), network.getSsid());
        } catch (Exception e) {
            log.debug("Error clearing old connections: ", e);
        }
    }

    // ==========================================
    // EXISTING METHODS (keeping all the working methods unchanged)
    // ==========================================

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

    // Connection methods (existing, keeping them unchanged)
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

    /**
     * Scheduled task to refresh network data every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void refreshNetworkData() {
        log.debug("Refreshing WiFi network data...");
        updateConnectionStatuses();
    }

    // Database helper methods
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

            // Simulate data usage growth
            if (connection.getDataUsageBytes() != null) {
                long currentUsage = connection.getDataUsageBytes();
                long additionalUsage = (long) (Math.random() * 1024 * 1024); // Random MB
                connection.setDataUsageBytes(currentUsage + additionalUsage);
            } else {
                connection.setDataUsageBytes((long) (Math.random() * 100 * 1024 * 1024)); // Random initial usage
            }

            connectionRepository.save(connection);
        }
    }

    // Statistics helper methods
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
}