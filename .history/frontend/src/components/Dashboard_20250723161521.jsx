import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Line, Bar } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, TimeScale } from 'chart.js';
import 'chartjs-adapter-date-fns';
import './Dashboard.css';

// Register Chart.js components
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, TimeScale);

// --- MOCK DATA (As backend is not yet connected) ---
const mockDevices = [
    { id: 1, name: "Raiyan's iPhone", type: "mobile", status: "online", user: "Raiyan Islam", ip: "192.168.1.10", blocked: false, profileImg: "https://i.pravatar.cc/150?u=raiyan" },
    { id: 2, "name": "Al's Gaming PC", type: "desktop", status: "online", user: "Al Imam Uddin", ip: "192.168.1.12", blocked: false, profileImg: "https://i.pravatar.cc/150?u=al" },
    { id: 3, name: "Mihi's iPad Pro", type: "tablet", status: "offline", user: "Rukaiya Jahan", ip: "192.168.1.15", blocked: true, profileImg: "https://i.pravatar.cc/150?u=mihi" },
    { id: 4, name: "Living Room TV", type: "tv", status: "online", user: "Family", ip: "192.168.1.20", blocked: false, profileImg: "https://i.pravatar.cc/150?u=family" }
];

const mockActivityLog = [
    { id: 1, deviceId: 2, timestamp: new Date(Date.now() - 2 * 60000), site: "youtube.com", deviceName: "Al's Gaming PC" },
    { id: 2, deviceId: 1, timestamp: new Date(Date.now() - 5 * 60000), site: "instagram.com", deviceName: "Raiyan's iPhone" },
    { id: 3, deviceId: 4, timestamp: new Date(Date.now() - 10 * 60000), site: "netflix.com", deviceName: "Living Room TV" },
    { id: 4, deviceId: 2, timestamp: new Date(Date.now() - 12 * 60000), site: "github.com", deviceName: "Al's Gaming PC" },
];

const mockUsageData = {
    '7d': { labels: ['YouTube', 'Roblox', 'TikTok', 'Netflix', 'Other'], data: [15, 12, 9, 7, 5] },
    '15d': { labels: ['YouTube', 'Roblox', 'TikTok', 'Netflix', 'Other'], data: [35, 28, 22, 18, 10] },
    '30d': { labels: ['YouTube', 'Roblox', 'TikTok', 'Netflix', 'Other'], data: [70, 60, 45, 30, 25] },
};

// --- Helper Functions & Components ---
const getDeviceIcon = (type) => {
    const icons = { mobile: '📱', desktop: '🖥️', tablet: 'iPad', tv: '📺', laptop: '💻' };
    return icons[type] || '🔌';
};

const Modal = ({ children, onClose }) => (
    <div className="modal-backdrop" onClick={onClose}>
        <div className="modal-content" onClick={e => e.stopPropagation()}>
            <button className="modal-close-btn" onClick={onClose}>&times;</button>
            {children}
        </div>
    </div>
);

const Dashboard = () => {
    const [devices, setDevices] = useState([]);
    const [activityLog, setActivityLog] = useState([]);
    const [loading, setLoading] = useState(true);
    const [parentName, setParentName] = useState('');
    const [currentTime, setCurrentTime] = useState(new Date());
    const navigate = useNavigate();

    // State for new features
    const [modal, setModal] = useState({ type: null, data: null });
    const [usageFilter, setUsageFilter] = useState('7d');
    const [historicalFilter, setHistoricalFilter] = useState('1d');

    const checkAuth = useCallback(() => {
        if (!localStorage.getItem('token')) navigate('/login');
    }, [navigate]);

    const fetchData = useCallback(() => {
        setLoading(true);
        // Simulate API calls
        setTimeout(() => {
            setDevices(mockDevices);
            setActivityLog(mockActivityLog);
            setParentName(localStorage.getItem('parentName') || 'Parent');
            setLoading(false);
        }, 1200);
    }, []);

    useEffect(() => {
        checkAuth();
        fetchData();
        const timeInterval = setInterval(() => setCurrentTime(new Date()), 1000);
        return () => clearInterval(timeInterval);
    }, [checkAuth, fetchData]);

    const toggleDeviceBlock = (deviceId) => {
        setDevices(devices.map(d => d.id === deviceId ? { ...d, blocked: !d.blocked } : d));
        // In a real app, an API call would be made here.
    };

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('parentName');
        navigate('/login');
    };

    // --- Chart Data ---
    const usageChartData = {
        labels: mockUsageData[usageFilter].labels,
        datasets: [{
            label: `Time Spent (hours) - Last ${usageFilter.replace('d', ' Days')}`,
            data: mockUsageData[usageFilter].data,
            backgroundColor: ['#7b68ee', '#6a5acd', '#836fff', '#9370db', '#c0b6ff'],
            borderColor: 'white',
            borderWidth: 2,
        }],
    };

    const formatTime = (time) => time.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', second: '2-digit', hour12: true });

    if (loading) {
        return <div className="loading-container"><div className="spinner"></div><p>Loading NetGuard Dashboard...</p></div>;
    }

    return (
        <div className="dashboard-container">
            {/* --- Modals --- */}
            {modal.type && <Modal onClose={() => setModal({ type: null, data: null })}>
                {modal.type === 'schedule' && (
                    <>
                        <h2>🗓️ Set Block Schedule for {modal.data.name}</h2>
                        <p>Define times when internet access should be blocked.</p>
                        <div className="schedule-form">
                            <label>Block internet after:</label>
                            <input type="time" defaultValue="23:00" />
                            <label>Unblock internet at:</label>
                            <input type="time" defaultValue="07:00" />
                            <button className="modal-action-btn">Save Schedule</button>
                        </div>
                    </>
                )}
                {modal.type === 'profile' && <h2>👤 Profile Management</h2>}
                {modal.type === 'history' && (
                    <>
                        <h2>Browse History: {modal.data.name}</h2>
                        <div className="filter-tabs">
                            {['1d', '3d', '7d', '15d', '30d'].map(f => (
                                <button key={f} className={historicalFilter === f ? 'active' : ''} onClick={() => setHistoricalFilter(f)}>
                                    {f.replace('d', ' Day')}
                                </button>
                            ))}
                        </div>
                        <ul className="historical-log">
                            <li>youtube.com<span>2h ago</span></li>
                            <li>google.com<span>3h ago</span></li>
                        </ul>
                    </>
                )}
            </Modal>}

            {/* --- Header --- */}
            <header className="dashboard-header">
                <div className="header-left">
                    <h1>🛡️ NetGuard</h1>
                    <span className="current-time">{formatTime(currentTime)}</span>
                </div>
                <div className="header-right">
                    <span className="welcome-text">Welcome, {parentName}</span>
                    <button onClick={() => setModal({ type: 'profile' })} className="profile-btn">👤</button>
                    <button onClick={handleLogout} className="logout-btn">Logout</button>
                </div>
            </header>

            <main className="dashboard-main">
                {/* --- Stats Cards --- */}
                <div className="dashboard-stats">
                    <div className="stat-card"><h3>Total Devices</h3><div className="stat-number">{devices.length}</div></div>
                    <div className="stat-card"><h3>Active Now</h3><div className="stat-number">{devices.filter(d => d.status === 'online').length}</div></div>
                    <div className="stat-card"><h3>Blocked</h3><div className="stat-number">{devices.filter(d => d.blocked).length}</div></div>
                    <div className="stat-card"><h3>Alerts</h3><div className="stat-number">0</div></div>
                </div>

                {/* --- Devices Section --- */}
                <section className="content-section">
                    <div className="section-header"><h2>Connected Devices</h2><button onClick={fetchData} className="refresh-btn">🔄 Refresh</button></div>
                    <div className="devices-grid">
                        {devices.map(device => (
                            <div key={device.id} className={`device-card ${device.blocked ? 'blocked' : ''}`}>
                                <div className="device-header">
                                    <img src={device.profileImg} alt={device.user} className="device-user-avatar" />
                                    <div className="device-info">
                                        <h3>{device.name}</h3>
                                        <span>{device.user} - {getDeviceIcon(device.type)}</span>
                                    </div>
                                    <span className={`status ${device.status}`}>{device.status}</span>
                                </div>
                                <div className="device-controls">
                                    <button onClick={() => toggleDeviceBlock(device.id)} className={`control-btn ${device.blocked ? 'unblock' : 'block'}`}>
                                        {device.blocked ? '✅ Unblock' : '🚫 Block'}
                                    </button>
                                    <button onClick={() => setModal({ type: 'schedule', data: device })} className="control-btn schedule">🗓️ Schedule</button>
                                    <button onClick={() => setModal({ type: 'history', data: device })} className="control-btn history">📜 History</button>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>

                {/* --- Activity & Reports --- */}
                <div className="dual-column-section">
                    <section className="content-section">
                        <div className="section-header"><h2>Real-Time Activity Log</h2></div>
                        <ul className="activity-log">
                            {activityLog.map(log => (
                                <li key={log.id}>
                                    <span>{log.deviceName}</span>
                                    <a href={`http://${log.site}`} target="_blank" rel="noopener noreferrer">{log.site}</a>
                                    <span className="log-time">{formatTime(log.timestamp)}</span>
                                </li>
                            ))}
                        </ul>
                    </section>
                    <section className="content-section">
                        <div className="section-header">
                            <h2>Weekly Usage Reports</h2>
                            <div className="filter-tabs">
                                {['7d', '15d', '30d'].map(f => (
                                    <button key={f} className={usageFilter === f ? 'active' : ''} onClick={() => setUsageFilter(f)}>
                                        {f.replace('d', ' Days')}
                                    </button>
                                ))}
                            </div>
                        </div>
                        <div className="chart-container">
                            <Bar data={usageChartData} options={{ maintainAspectRatio: false, responsive: true }} />
                        </div>
                    </section>
                </div>
            </main>
        </div>
    );
};

export default Dashboard;