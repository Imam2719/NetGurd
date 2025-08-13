import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './Dashboard.css';

const Dashboard = () => {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [parentName, setParentName] = useState('');
  const [currentTime, setCurrentTime] = useState(new Date());
  const navigate = useNavigate();

  // Mock data for development (replace with real API calls)
  const mockDevices = [
    { id: 1, name: "Sarah's iPhone", type: "mobile", status: "online", timeSpent: "2h 15m", blocked: false },
    { id: 2, name: "Gaming PC", type: "desktop", status: "online", timeSpent: "4h 30m", blocked: false },
    { id: 3, name: "iPad Pro", type: "tablet", status: "offline", timeSpent: "1h 45m", blocked: true },
    { id: 4, name: "Smart TV", type: "tv", status: "online", timeSpent: "3h 20m", blocked: false }
  ];

  useEffect(() => {
    checkAuth();
    fetchDevices();
    setParentName(localStorage.getItem('parentName') || 'Parent');

    // Update time every minute
    const timeInterval = setInterval(() => setCurrentTime(new Date()), 60000);

    return () => clearInterval(timeInterval);
  }, []);

  const checkAuth = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
    }
  };

  const fetchDevices = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');

      // Uncomment when backend is ready:
      // const response = await axios.get('http://localhost:8080/api/devices', {
      //   headers: { Authorization: `Bearer ${token}` }
      // });
      // setDevices(response.data);

      // Using mock data for now:
      setTimeout(() => {
        setDevices(mockDevices);
        setLoading(false);
      }, 1000);

    } catch (error) {
      console.error('Failed to fetch devices');
      setDevices(mockDevices); // Fallback to mock data
      setLoading(false);
    }
  };

  const toggleDeviceBlock = async (deviceId) => {
    try {
      setDevices(devices.map(device =>
        device.id === deviceId
          ? { ...device, blocked: !device.blocked }
          : device
      ));

      // TODO: Make API call to backend
      // await axios.post(`http://localhost:8080/api/devices/${deviceId}/toggle-block`);

    } catch (error) {
      console.error('Failed to toggle device block:', error);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('parentName');
    navigate('/login');
  };

  const getDeviceIcon = (type) => {
    const icons = {
      mobile: '📱',
      desktop: '🖥️',
      tablet: '📊',
      tv: '📺',
      laptop: '💻'
    };
    return icons[type] || '🔌';
  };

  const formatTime = (time) => {
    return time.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });
  };

  if (loading) {
    return (
      <div className="dashboard-container">
        <div className="loading">
          <div className="spinner"></div>
          <p>Loading NetGuard Dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="header-left">
          <h1>🛡️ NetGuard</h1>
          <span className="current-time">{formatTime(currentTime)}</span>
        </div>
        <div className="header-right">
          <span className="welcome-text">Welcome back, {parentName}</span>
          <button onClick={handleLogout} className="logout-btn">Logout</button>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="dashboard-stats">
          <div className="stat-card">
            <h3>Total Devices</h3>
            <div className="stat-number">{devices.length}</div>
          </div>
          <div className="stat-card">
            <h3>Active Now</h3>
            <div className="stat-number">{devices.filter(d => d.status === 'online').length}</div>
          </div>
          <div className="stat-card">
            <h3>Blocked</h3>
            <div className="stat-number">{devices.filter(d => d.blocked).length}</div>
          </div>
          <div className="stat-card">
            <h3>Protected</h3>
            <div className="stat-number">100%</div>
          </div>
        </div>

        <section className="devices-section">
          <div className="section-header">
            <h2>Connected Devices</h2>
            <button onClick={fetchDevices} className="refresh-btn">🔄 Refresh</button>
          </div>

          <div className="devices-grid">
            {devices.map(device => (
              <div key={device.id} className={`device-card ${device.blocked ? 'blocked' : ''}`}>
                <div className="device-header">
                  <div className="device-icon">{getDeviceIcon(device.type)}</div>
                  <div className="device-info">
                    <h3>{device.name}</h3>
                    <span className={`status ${device.status}`}>
                      {device.status === 'online' ? '🟢' : '⚫'} {device.status}
                    </span>
                  </div>
                </div>

                <div className="device-stats">
                  <div className="time-spent">
                    <span>Today: {device.timeSpent}</span>
                  </div>
                </div>

                <div className="device-controls">
                  <button
                    onClick={() => toggleDeviceBlock(device.id)}
                    className={`control-btn ${device.blocked ? 'unblock' : 'block'}`}
                  >
                    {device.blocked ? '✅ Unblock Internet' : '🚫 Block Internet'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="quick-actions">
          <h2>Quick Actions</h2>
          <div className="actions-grid">
            <button className="action-btn">
              <span className="action-icon">⏰</span>
              <span>Set Schedule</span>
            </button>
            <button className="action-btn">
              <span className="action-icon">📊</span>
              <span>View Reports</span>
            </button>
            <button className="action-btn">
              <span className="action-icon">🔒</span>
              <span>Block All</span>
            </button>
            <button className="action-btn">
              <span className="action-icon">⚙️</span>
              <span>Settings</span>
            </button>
          </div>
        </section>
      </main>
    </div>
  );
};

export default Dashboard;