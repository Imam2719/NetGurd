# NetGuard 🛡️

**A Smart Parental Internet Control System**

NetGuard is an intelligent network management solution designed to help parents control and monitor their children's internet usage. With automated scheduling, real-time monitoring, and comprehensive device management, NetGuard promotes healthy digital habits for the entire family.


## 🌟 Features

### Core Functionality
- **🕒 Automatic Internet Scheduling** - Set custom internet cut-off times (e.g., after midnight)
- **📱 Multi-Device Management** - Control and monitor all connected devices
- **🌐 Real-Time Network Monitoring** - Live tracking of device activity and browsing
- **⏰ Time Limits** - Set daily/weekly screen time limits per device
- **🚫 Instant Block/Unblock** - Manual override controls for immediate action
- **📊 Usage Analytics** - Comprehensive reports on internet usage patterns

### Advanced Features
- **🔍 Enhanced Device Discovery** - Automatic detection and authentic naming of network devices
- **🌐 Website Monitoring** - Track browsing history and current website activity
- **🔒 Security Scanning** - Real-time threat detection and security alerts
- **📈 Traffic Analysis** - Detailed network performance and data usage metrics
- **🎯 Custom Profiles** - Individual settings for each family member
- **📱 Cross-Platform Dashboard** - Web and mobile-friendly interface

## 🛠️ Technology Stack

- **Frontend**: React.js
- **Backend**: Java Spring Boot
- **Database**: PostgreSQL
- **Network Control**: System firewall integration (iptables/Windows Firewall)
- **Real-time Monitoring**: WebSocket connections
- **Device Discovery**: Multi-protocol scanning (ARP, mDNS, SNMP, UPnP)

## 📋 Prerequisites

- Java 11 or higher
- Node.js 14+ and npm
- PostgreSQL 12+
- Administrative privileges (for network control features)
- Compatible operating system: Windows 10+, macOS 10.15+, Linux (Ubuntu 18.04+)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/Imam2719/NetGurd.git
cd NetGurd
```

### 2. Backend Setup
```bash
cd Dashboard_Features_Backend
./mvnw clean install
./mvnw spring-boot:run
```

### 3. Frontend Setup
```bash
cd netguard-frontend
npm install
npm start
```

### 4. Database Configuration
Create a PostgreSQL database and update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/netguard
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## 📖 Usage

### Initial Setup
1. **Network Scan**: NetGuard automatically discovers all devices on your network
2. **Device Identification**: The system uses advanced techniques to identify device types and names
3. **Profile Creation**: Set up individual profiles for family members
4. **Schedule Configuration**: Define internet access schedules and time limits

### Daily Operations
- **Dashboard Overview**: Monitor all connected devices and their current activity
- **Real-time Controls**: Block/unblock devices instantly when needed
- **Usage Reports**: Review daily, weekly, and monthly internet usage patterns
- **Security Alerts**: Receive notifications about suspicious activities or security threats

## 🏗️ Project Structure

```
NetGurd/
├── Dashboard_Features_Backend/          # Java Spring Boot backend
│   ├── src/main/java/.../Service/      # Core business logic services
│   │   ├── DeviceAnalyticsService      # Device usage analytics
│   │   ├── DeviceManagementService     # Device control and blocking
│   │   ├── Overview_AvailableNetwork_service  # Network scanning
│   │   ├── RealTimeMonitoringService   # Live monitoring
│   │   └── RealWebsiteMonitoringService # Website tracking
│   ├── src/main/java/.../DTO/          # Data Transfer Objects
│   ├── src/main/java/.../Entity/       # Database entities
│   └── src/main/java/.../Repository/   # Data access layer
├── netguard-frontend/                  # React.js frontend
├── assets/                             # Images and documentation
└── README.md
```

## 🖼️ Screenshots

### Main Dashboard
![Dashboard](./assets/dashboard-screenshot.png)
*Overview of connected devices and network status*

### Device Management
![Device Management](./assets/device-management-screenshot.png)
*Individual device controls and settings*

### Analytics Dashboard
![Analytics](./assets/analytics-screenshot.png)
*Usage statistics and reports*

### Network Scanner
![Network Scanner](./assets/network-scanner-screenshot.png)
*Real-time network device discovery*

## 🔧 Configuration

### Network Control Settings
NetGuard supports multiple network control methods:
- **Linux**: iptables integration
- **Windows**: Windows Firewall rules
- **macOS**: Built-in firewall controls

### Security Configuration
- Enable/disable automatic threat detection
- Configure blocked website categories
- Set up security alert notifications
- Customize privacy settings

## 🤝 Contributing

We welcome contributions to NetGuard! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit your changes**: `git commit -m 'Add amazing feature'`
4. **Push to the branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Development Guidelines
- Follow Java coding standards for backend development
- Use ESLint and Prettier for frontend code formatting
- Write unit tests for new features
- Update documentation for any API changes

## 🐛 Known Issues

- Device blocking may not work on all router configurations
- Some mobile devices may bypass controls using cellular data
- VPN usage can circumvent website monitoring
- Initial device discovery may take several minutes on large networks

## 🔮 Future Roadmap

- [ ] Mobile app for iOS and Android
- [ ] AI-powered content categorization
- [ ] Integration with school calendars
- [ ] Gamified rewards system for balanced usage
- [ ] Public Wi-Fi control features
- [ ] Advanced reporting and analytics
- [ ] Multi-location support

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Developer 
- **Al Imam Uddin** (ID: 2021-3-60-260)


*Computer Science & Engineering Department*  
*East West University*

## 🆘 Support

If you encounter any issues or have questions:

1. Check our [Issues](https://github.com/Imam2719/NetGurd/issues) page
2. Create a new issue with detailed information
3. Contact the development team

## 📞 Contact

For technical support or collaboration inquiries, please reach out through GitHub Issues or contact the development team directly.
![WhatsApp Image 2025-09-03 at 12 42 43_2845898d](https://github.com/user-attachments/assets/f651e7ea-00fa-46bf-87df-337ba6c4ac4c)


![WhatsApp Image 2025-09-03 at 12 43 31_aba090eb](https://github.com/user-attachments/assets/598dc91a-fbf8-4ddb-8b16-25ff30f5b7c9)


![WhatsApp Image 2025-09-03 at 12 43 48_8daef21c](https://github.com/user-attachments/assets/420c2721-316f-4c8e-8a33-363b1e571f25)



![WhatsApp Image 2025-09-03 at 12 44 33_ed7c945c](https://github.com/user-attachments/assets/80a69565-a96c-43b5-9117-8e0f05e0fff4)


![WhatsApp Image 2025-09-03 at 12 44 56_368292c4](https://github.com/user-attachments/assets/cf1d79b7-a69b-473d-9688-06b58839a3d7)


![WhatsApp Image 2025-09-03 at 12 45 17_005b8fa6](https://github.com/user-attachments/assets/320966f8-aa93-49a0-ac07-dfc4b339092f)


![WhatsApp Image 2025-09-03 at 12 45 44_4d8f66fe](https://github.com/user-attachments/assets/aa7c1060-63a5-4ea0-be8a-b117f099891b)


![WhatsApp Image 2025-09-03 at 12 46 15_28345bb6](https://github.com/user-attachments/assets/1ed2ecf3-3b7b-499e-943a-75969d754fed)



![WhatsApp Image 2025-09-03 at 12 46 41_216af027](https://github.com/user-attachments/assets/d0b3eea6-01b0-4e0d-a07f-236a1976f95a)



![WhatsApp Image 2025-09-03 at 12 47 16_60f8b497](https://github.com/user-attachments/assets/2f8bcb07-2c18-45ab-bd8a-6bff7810e2cd)










