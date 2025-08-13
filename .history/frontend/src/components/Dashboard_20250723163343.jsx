// Dashboard.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Line, Bar } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, TimeScale } from 'chart.js';
import 'chartjs-adapter-date-fns';
import './Dashboard.css';

// Register Chart.js components
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, TimeScale);

// --- MOCK DATA (As backend is not yet connected) ---
// Enhanced mock devices to include browse history for demo purposes
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
    { id: 5, deviceId: 1, timestamp: new Date(Date.now() - 20 * 60000), site: "facebook.com", deviceName: "Raiyan's iPhone" },
    { id: 6, deviceId: 3, timestamp: new Date(Date.now() - 30 * 60000), site: "blocked-site.com", deviceName: "Mihi's iPad Pro" },
];

const mockUsageData = {
    '7d': { labels: ['YouTube', 'Roblox', 'TikTok', 'Netflix', 'Other'], data: [15, 12, 9, 7, 5] },
    '15d': { labels: ['YouTube', 'Roblox', 'TikTok', 'Netflix', 'Other'], data: [35, 28, 22, 18, 10] },
    '30d': { labels: ['YouTube', 'Roblox', 'TikTok', 'Netflix', 'Other'], data: [70, 60, 45, 30, 25] },
};

// Mock historical Browse data for a specific device
const mockHistoricalBrowse = (deviceId, filter) => {
    const now = Date.now();
    let days = 1;
    if (filter === '3d') days = 3;
    else if (filter === '7d') days = 7;
    else if (filter === '15d') days = 15;
    else if (filter === '30d') days = 30;

    const filteredLogs = mockActivityLog.filter(log =>
        log.deviceId === deviceId && (now - log.timestamp.getTime()) < (days * 24 * 60 * 60 * 1000)
    ).concat([
        // Add more synthetic historical data for richer demo
        { site: "google.com", timestamp: new Date(now - (days / 2) * 24 * 60 * 60 * 1000) },
        { site: "wikipedia.org", timestamp: new Date(now - (days / 3) * 24 * 60 * 60 * 1000) },
        { site: "stackoverflow.com", timestamp: new Date(now - (days / 4) * 24 * 60 * 60 * 1000) },
    ]).sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime()); // Sort by most recent

    return filteredLogs;
};


// --- Helper Functions & Components ---
const getDeviceIcon = (type) => {
    const icons = { mobile: '📱', desktop: '🖥️', tablet: 'タブレット', tv: '📺', laptop: '💻' };
    return icons[type] || '🔌';
};

const Modal = ({ children, onClose }) => (
    <div className="modal-backdrop" onClick={onClose}>
        <div className="modal-content" onClick={e => e.stopPropagation()}>
            <button className="modal-close-btn" onClick={onClose}>×</button>
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
    const [historicalFilter, setHistoricalFilter] = useState('1d'); // Default to 1 day
    const [scheduleTimes, setScheduleTimes] = useState({ block: '23:00', unblock: '07:00' });


    const checkAuth = useCallback(() => {
        if (!localStorage.getItem('token')) {
            // In a real app, this would be more robust (e.g., token validation with backend)
            navigate('/login');
        }
    }, [navigate]);

    const fetchData = useCallback(() => {
        setLoading(true);
        // Simulate API calls
        setTimeout(() => {
            setDevices(mockDevices);
            setActivityLog(mockActivityLog);
            setParentName(localStorage.getItem('parentName') || 'Parent'); // Default to 'Parent' if not set
            setLoading(false);
        }, 1200); // Simulate network delay
    }, []);

    useEffect(() => {
        checkAuth();
        fetchData();
        const timeInterval = setInterval(() => setCurrentTime(new Date()), 1000);
        return () => clearInterval(timeInterval);
    }, [checkAuth, fetchData]);

    // Expected Feature: Manual internet toggle for each device
    const toggleDeviceBlock = (deviceId) => {
        setDevices(devices.map(d => d.id === deviceId ? { ...d, blocked: !d.blocked } : d));
        // In a real app, an API call would be made here to update device status on the router/proxy.
        console.log(`Toggling block for device ${deviceId}`);
    };

    // Expected Feature: Time-based internet blocking (via Schedule Modal)
    const handleScheduleSave = (deviceId) => {
        console.log(`Saving schedule for device ${deviceId}: Block from ${scheduleTimes.block} to ${scheduleTimes.unblock}`);
        // Here, an API call would be made to set the schedule for the device.
        setModal({ type: null, data: null }); // Close modal after saving
    };

    // Expected Feature: Parental log in account/profile management
    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('parentName');
        navigate('/login');
    };

    // Chart Data for Weekly Usage Reports
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

    // Format time for display
    const formatTime = (time) => time.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', second: '2-digit', hour12: true });
    const formatDate = (date) => date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    const formatDateTime = (date) => `${formatDate(date)} ${formatTime(date)}`;


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
                        <p>Define times when internet access should be blocked for this device.</p>
                        <div className="schedule-form">
                            <label htmlFor="block-time">Block internet after:</label>
                            <input
                                id="block-time"
                                type="time"
                                value={scheduleTimes.block}
                                onChange={(e) => setScheduleTimes({ ...scheduleTimes, block: e.target.value })}
                            />
                            <label htmlFor="unblock-time">Unblock internet at:</label>
                            <input
                                id="unblock-time"
                                type="time"
                                value={scheduleTimes.unblock}
                                onChange={(e) => setScheduleTimes({ ...scheduleTimes, unblock: e.target.value })}
                            />
                            <button className="modal-action-btn" onClick={() => handleScheduleSave(modal.data.id)}>Save Schedule</button>
                        </div>
                    </>
                )}
                {modal.type === 'profile' && (
                    <>
                        <h2>👤 Profile Management</h2>
                        <p>Manage your parental account settings.</p>
                        {/* Placeholder for profile management features */}
                        <div className="schedule-form">
                            <label>Parent Name:</label>
                            <input type="text" defaultValue={parentName} readOnly />
                            <label>Email:</label>
                            <input type="email" defaultValue="parent@example.com" readOnly />
                            <button className="modal-action-btn">Edit Profile</button>
                        </div>
                    </>
                )}
                {modal.type === 'history' && (
                    <>
                        <h2>📜 Browse History: {modal.data.name}</h2>
                        <p>View historical Browse activity for this device.</p>
                        <div className="filter-tabs">
                            {['1d', '3d', '7d', '15d', '30d'].map(f => (
                                <button key={f} className={historicalFilter === f ? 'active' : ''} onClick={() => setHistoricalFilter(f)}>
                                    {f.replace('d', ' Day')}
                                </button>
                            ))}
                        </div>
                        <ul className="historical-log">
                            {mockHistoricalBrowse(modal.data.id, historicalFilter).length > 0 ? (
                                mockHistoricalBrowse(modal.data.id, historicalFilter).map((log, index) => (
                                    <li key={index}>
                                        <span>{log.site}</span>
                                        <span>{formatDateTime(log.timestamp)}</span>
                                    </li>
                                ))
                            ) : (
                                <li>No Browse history available for this period.</li>
                            )}
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
                    <button onClick={() => setModal({ type: 'profile' })} className="profile-btn" aria-label="Profile Management">👤</button>
                    {/* Add a settings button as an example */}
                    <button onClick={() => console.log("Open settings")} className="settings-btn" aria-label="Settings">⚙️</button>
                    <button onClick={handleLogout} className="logout-btn">Logout</button>
                </div>
            </header>

            <main className="dashboard-main">
                {/* --- Stats Cards --- */}
                <div className="dashboard-stats">
                    <div className="stat-card"><h3>Total Devices</h3><div className="stat-number">{devices.length}</div></div>
                    <div className="stat-card"><h3>Active Now</h3><div className="stat-number">{devices.filter(d => d.status === 'online').length}</div></div>
                    <div className="stat-card"><h3>Blocked</h3><div className="stat-number">{devices.filter(d => d.blocked).length}</div></div>
                    <div className="stat-card"><h3>Alerts</h3><div className="stat-number">0</div></div> {/* Placeholder for alerts */}
                </div>

                {/* --- Devices Section (Manual internet toggle for each device, Custom block schedules per device/user) --- */}
                <section className="content-section">
                    <div className="section-header">
                        <h2>Connected Devices</h2>
                        <button onClick={fetchData} className="refresh-btn">🔄 Refresh Devices</button>
                    </div>
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
                                        {device.blocked ? '✅ Unblock' : '🚫 Block Now'}
                                    </button>
                                    <button onClick={() => setModal({ type: 'schedule', data: device })} className="control-btn schedule">🗓️ Schedule Block</button>
                                    <button onClick={() => setModal({ type: 'history', data: device })} className="control-btn history">📜 Browse History</button>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>

                {/* --- Activity & Reports --- */}
                <div className="dual-column-section">
                    {/* Expected Feature: Real-time activity logs (in router how many device connected and they are current which site Browse) */}
                    <section className="content-section">
                        <div className="section-header"><h2>Real-Time Activity Log</h2></div>
                        <ul className="activity-log">
                            {activityLog.length > 0 ? (
                                activityLog.map(log => (
                                    <li key={log.id}>
                                        <span>{log.deviceName}</span>
                                        <a href={`http://${log.site}`} target="_blank" rel="noopener noreferrer">{log.site}</a>
                                        <span className="log-time">{formatTime(log.timestamp)}</span>
                                    </li>
                                ))
                            ) : (
                                <li>No recent activity to display.</li>
                            )}
                        </ul>
                    </section>
                    {/* Expected Feature: Weekly usage reports and visual charts */}
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