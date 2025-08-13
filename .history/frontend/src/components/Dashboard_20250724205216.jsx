import React, { useState, useEffect, useCallback } from 'react';
import { Line, Bar, Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, TimeScale, ArcElement } from 'chart.js';
import 'chartjs-adapter-date-fns';

// Register Chart.js components
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, TimeScale, ArcElement);

// Enhanced mock data
const mockDevices = [
  { id: 1, name: "Raiyan's iPhone", type: "mobile", status: "online", user: "Raiyan Islam", ip: "192.168.1.10", blocked: false, battery: 85, dataUsage: 2.4 },
  { id: 2, name: "Al's Gaming PC", type: "desktop", status: "online", user: "Al Imam Uddin", ip: "192.168.1.12", blocked: false, battery: null, dataUsage: 15.2 },
  { id: 3, name: "Mihi's iPad Pro", type: "tablet", status: "offline", user: "Rukaiya Jahan", ip: "192.168.1.15", blocked: true, battery: 42, dataUsage: 1.8 },
  { id: 4, name: "Living Room TV", type: "tv", status: "online", user: "Family", ip: "192.168.1.20", blocked: false, battery: null, dataUsage: 8.7 },
  { id: 5, name: "Dad's Laptop", type: "laptop", status: "online", user: "Father", ip: "192.168.1.25", blocked: false, battery: 67, dataUsage: 4.3 }
];

const mockRealtimeActivity = [
  { deviceId: 1, site: "youtube.com/watch?v=tech_review", deviceName: "Raiyan's iPhone", timestamp: new Date(), category: "Entertainment" },
  { deviceId: 2, site: "discord.com/gaming-channel", deviceName: "Al's Gaming PC", timestamp: new Date(), category: "Social" },
  { deviceId: 4, site: "netflix.com/movie/action", deviceName: "Living Room TV", timestamp: new Date(), category: "Streaming" },
  { deviceId: 5, site: "github.com/project/dashboard", deviceName: "Dad's Laptop", timestamp: new Date(), category: "Work" }
];

const mockUsageData = {
  '7d': { labels: ['YouTube', 'Gaming', 'Netflix', 'Social Media', 'Work', 'Education'], data: [15, 12, 9, 7, 5, 3] },
  '15d': { labels: ['YouTube', 'Gaming', 'Netflix', 'Social Media', 'Work', 'Education'], data: [35, 28, 22, 18, 10, 8] },
  '30d': { labels: ['YouTube', 'Gaming', 'Netflix', 'Social Media', 'Work', 'Education'], data: [70, 60, 45, 30, 25, 15] },
};

const mockNetworkStats = {
  totalBandwidth: 100,
  usedBandwidth: 67,
  peakHours: "8:00 PM - 10:00 PM",
  averageSpeed: "85.2 Mbps"
};

const getDeviceIcon = (type) => {
  const icons = { 
    mobile: '📱', 
    desktop: '🖥️', 
    tablet: '📟', 
    tv: '📺', 
    laptop: '💻' 
  };
  return icons[type] || '🔌';
};

const getCategoryIcon = (category) => {
  const icons = {
    'Entertainment': '🎬',
    'Social': '💬',
    'Streaming': '📺',
    'Work': '💼',
    'Education': '📚',
    'Gaming': '🎮'
  };
  return icons[category] || '🌐';
};

const Modal = ({ children, onClose }) => (
  <div className="modal-backdrop" onClick={onClose}>
    <div className="modern-modal" onClick={e => e.stopPropagation()}>
      <button className="modal-close" onClick={onClose}>✕</button>
      {children}
    </div>
  </div>
);

const Dashboard = () => {
  const [devices, setDevices] = useState([]);
  const [currentActivityLog, setCurrentActivityLog] = useState([]);
  const [loading, setLoading] = useState(true);
  const [parentName, setParentName] = useState('');
  const [currentTime, setCurrentTime] = useState(new Date());
  const [modal, setModal] = useState({ type: null, data: null });
  const [usageFilter, setUsageFilter] = useState('7d');
  const [historicalFilter, setHistoricalFilter] = useState('1d');
  const [scheduleTimes, setScheduleTimes] = useState({ block: '23:00', unblock: '07:00' });
  const [activeTab, setActiveTab] = useState('overview');

  const fetchData = useCallback(() => {
    setLoading(true);
    setTimeout(() => {
      setDevices(mockDevices);
      setCurrentActivityLog(mockRealtimeActivity);
      setParentName('John Smith');
      setLoading(false);
    }, 1200);
  }, []);

  useEffect(() => {
    fetchData();
    const timeInterval = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timeInterval);
  }, [fetchData]);

  const toggleDeviceBlock = (deviceId) => {
    setDevices(devices.map(d => d.id === deviceId ? { ...d, blocked: !d.blocked } : d));
  };

  const handleScheduleSave = (deviceId) => {
    console.log(`Saving schedule for device ${deviceId}`);
    setModal({ type: null, data: null });
  };

  const handleLogout = () => {
    console.log('Logging out...');
  };

  const formatTime = (time) => time.toLocaleTimeString('en-US', { 
    hour: 'numeric', 
    minute: '2-digit', 
    second: '2-digit', 
    hour12: true 
  });

  const usageChartData = {
    labels: mockUsageData[usageFilter].labels,
    datasets: [{
      label: `Hours - Last ${usageFilter.replace('d', ' Days')}`,
      data: mockUsageData[usageFilter].data,
      backgroundColor: [
        'rgba(106, 90, 205, 0.8)',
        'rgba(123, 104, 238, 0.8)',
        'rgba(147, 112, 219, 0.8)',
        'rgba(138, 43, 226, 0.8)',
        'rgba(75, 0, 130, 0.8)',
        'rgba(72, 61, 139, 0.8)'
      ],
      borderColor: [
        'rgba(106, 90, 205, 1)',
        'rgba(123, 104, 238, 1)',
        'rgba(147, 112, 219, 1)',
        'rgba(138, 43, 226, 1)',
        'rgba(75, 0, 130, 1)',
        'rgba(72, 61, 139, 1)'
      ],
      borderWidth: 2,
      borderRadius: 8,
    }],
  };

  const networkChartData = {
    labels: ['Used', 'Available'],
    datasets: [{
      data: [mockNetworkStats.usedBandwidth, mockNetworkStats.totalBandwidth - mockNetworkStats.usedBandwidth],
      backgroundColor: ['#6a5acd', '#e2e8f0'],
      borderWidth: 0,
    }],
  };

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        color: 'white'
      }}>
        <div style={{
          width: '80px',
          height: '80px',
          border: '8px solid rgba(255,255,255,0.3)',
          borderTop: '8px solid white',
          borderRadius: '50%',
          animation: 'spin 1s linear infinite',
          marginBottom: '2rem'
        }}></div>
        <h2 style={{ margin: 0, fontSize: '1.5rem', fontWeight: '600' }}>Loading NetGuard Dashboard...</h2>
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
      fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif"
    }}>
      <style jsx>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        @keyframes slideUp {
          from { transform: translateY(30px); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
        @keyframes pulse {
          0%, 100% { transform: scale(1); }
          50% { transform: scale(1.05); }
        }
        .modal-backdrop {
          position: fixed;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: rgba(0, 0, 0, 0.7);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 2000;
          backdrop-filter: blur(10px);
        }
        .modern-modal {
          background: white;
          padding: 2rem;
          border-radius: 20px;
          width: 90%;
          max-width: 600px;
          position: relative;
          box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25);
          animation: slideUp 0.3s ease-out;
        }
        .modal-close {
          position: absolute;
          top: 1rem;
          right: 1rem;
          background: none;
          border: none;
          font-size: 1.5rem;
          cursor: pointer;
          color: #666;
          width: 40px;
          height: 40px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.2s ease;
        }
        .modal-close:hover {
          background: #f0f0f0;
          transform: rotate(90deg);
        }
      `}</style>

      {/* Modals */}
      {modal.type && (
        <Modal onClose={() => setModal({ type: null, data: null })}>
          {modal.type === 'schedule' && (
            <div>
              <h2 style={{ color: '#6a5acd', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                🗓️ Schedule Block - {modal.data.name}
              </h2>
              <p style={{ color: '#666', marginBottom: '2rem' }}>
                Set automatic blocking times for this device.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    Block Time:
                  </label>
                  <input
                    type="time"
                    value={scheduleTimes.block}
                    onChange={(e) => setScheduleTimes({ ...scheduleTimes, block: e.target.value })}
                    style={{
                      padding: '0.75rem',
                      border: '2px solid #e2e8f0',
                      borderRadius: '12px',
                      fontSize: '1rem',
                      width: '100%',
                      transition: 'border-color 0.2s ease'
                    }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '600' }}>
                    Unblock Time:
                  </label>
                  <input
                    type="time"
                    value={scheduleTimes.unblock}
                    onChange={(e) => setScheduleTimes({ ...scheduleTimes, unblock: e.target.value })}
                    style={{
                      padding: '0.75rem',
                      border: '2px solid #e2e8f0',
                      borderRadius: '12px',
                      fontSize: '1rem',
                      width: '100%',
                      transition: 'border-color 0.2s ease'
                    }}
                  />
                </div>
                <button
                  onClick={() => handleScheduleSave(modal.data.id)}
                  style={{
                    background: 'linear-gradient(135deg, #6a5acd, #7b68ee)',
                    color: 'white',
                    border: 'none',
                    padding: '1rem 2rem',
                    borderRadius: '12px',
                    fontSize: '1rem',
                    fontWeight: '600',
                    cursor: 'pointer',
                    transition: 'transform 0.2s ease',
                    marginTop: '1rem'
                  }}
                  onMouseEnter={(e) => e.target.style.transform = 'translateY(-2px)'}
                  onMouseLeave={(e) => e.target.style.transform = 'translateY(0)'}
                >
                  Save Schedule
                </button>
              </div>
            </div>
          )}
        </Modal>
      )}

      {/* Header */}
      <header style={{
        background: 'rgba(255, 255, 255, 0.95)',
        backdropFilter: 'blur(20px)',
        padding: '1rem 2rem',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        position: 'sticky',
        top: 0,
        zIndex: 1000,
        borderBottom: '1px solid rgba(255, 255, 255, 0.2)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
          <h1 style={{
            background: 'linear-gradient(135deg, #6a5acd, #7b68ee)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            margin: 0,
            fontSize: '2rem',
            fontWeight: '700',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem'
          }}>
            🛡️ NetGuard Pro
          </h1>
          <div style={{
            background: 'linear-gradient(135deg, #6a5acd, #7b68ee)',
            color: 'white',
            padding: '0.75rem 1.5rem',
            borderRadius: '25px',
            fontSize: '0.9rem',
            fontWeight: '600',
            boxShadow: '0 4px 15px rgba(106, 90, 205, 0.3)'
          }}>
            {formatTime(currentTime)}
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <span style={{ color: '#666', fontWeight: '500' }}>Welcome back, {parentName}</span>
          <button
            onClick={handleLogout}
            style={{
              background: 'linear-gradient(135deg, #ff6b6b, #ee5a52)',
              color: 'white',
              border: 'none',
              padding: '0.75rem 1.5rem',
              borderRadius: '12px',
              cursor: 'pointer',
              fontWeight: '600',
              transition: 'transform 0.2s ease',
              boxShadow: '0 4px 15px rgba(255, 107, 107, 0.3)'
            }}
            onMouseEnter={(e) => e.target.style.transform = 'translateY(-2px)'}
            onMouseLeave={(e) => e.target.style.transform = 'translateY(0)'}
          >
            Logout
          </button>
        </div>
      </header>

      <main style={{ padding: '2rem', maxWidth: '1600px', margin: '0 auto' }}>
        {/* Stats Dashboard */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '2rem',
          marginBottom: '3rem'
        }}>
          {[
            { title: 'Total Devices', value: devices.length, icon: '📱', color: '#6a5acd', change: '+2 this week' },
            { title: 'Active Now', value: devices.filter(d => d.status === 'online').length, icon: '🟢', color: '#28a745', change: 'Peak: 8 PM' },
            { title: 'Blocked', value: devices.filter(d => d.blocked).length, icon: '🚫', color: '#dc3545', change: '1 scheduled' },
            { title: 'Data Usage', value: '32.4 GB', icon: '📊', color: '#17a2b8', change: '↑ 15% vs last week' },
            { title: 'Network Health', value: '98%', icon: '💚', color: '#28a745', change: 'Excellent' }
          ].map((stat, index) => (
            <div key={index} style={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              padding: '2rem',
              borderRadius: '24px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              transition: 'transform 0.3s ease, box-shadow 0.3s ease',
              cursor: 'pointer',
              animation: `slideUp 0.6s ease-out ${index * 0.1}s both`
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-8px)';
              e.currentTarget.style.boxShadow = '0 20px 40px rgba(0, 0, 0, 0.15)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0)';
              e.currentTarget.style.boxShadow = '0 8px 32px rgba(0, 0, 0, 0.1)';
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                <div style={{ fontSize: '2.5rem' }}>{stat.icon}</div>
                <div style={{
                  background: `${stat.color}20`,
                  color: stat.color,
                  padding: '0.5rem 1rem',
                  borderRadius: '12px',
                  fontSize: '0.8rem',
                  fontWeight: '600'
                }}>
                  {stat.change}
                </div>
              </div>
              <h3 style={{
                color: '#666',
                fontSize: '0.9rem',
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                margin: '0 0 0.5rem 0',
                fontWeight: '600'
              }}>
                {stat.title}
              </h3>
              <div style={{
                fontSize: '2.5rem',
                fontWeight: '700',
                color: stat.color,
                lineHeight: 1
              }}>
                {stat.value}
              </div>
            </div>
          ))}
        </div>

        {/* Main Content Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '2fr 1fr',
          gap: '2rem',
          marginBottom: '2rem'
        }}>
          {/* Devices Section */}
          <div style={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            padding: '2rem',
            borderRadius: '24px',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
            border: '1px solid rgba(255, 255, 255, 0.2)'
          }}>
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '2rem',
              paddingBottom: '1rem',
              borderBottom: '2px solid #f0f2f5'
            }}>
              <h2 style={{ margin: 0, fontSize: '1.8rem', color: '#333', fontWeight: '700' }}>
                Connected Devices
              </h2>
              <button
                onClick={fetchData}
                style={{
                  background: 'linear-gradient(135deg, #28a745, #20c997)',
                  color: 'white',
                  border: 'none',
                  padding: '0.75rem 1.5rem',
                  borderRadius: '12px',
                  cursor: 'pointer',
                  fontWeight: '600',
                  transition: 'transform 0.2s ease',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem'
                }}
                onMouseEnter={(e) => e.target.style.transform = 'translateY(-2px)'}
                onMouseLeave={(e) => e.target.style.transform = 'translateY(0)'}
              >
                🔄 Refresh
              </button>
            </div>
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))',
              gap: '1.5rem'
            }}>
              {devices.map(device => (
                <div key={device.id} style={{
                  background: device.blocked ? 'linear-gradient(135deg, #ffebee, #fce4ec)' : 'linear-gradient(135deg, #f8f9ff, #e8f5e8)',
                  border: `2px solid ${device.blocked ? '#ffcdd2' : '#c8e6c9'}`,
                  borderRadius: '20px',
                  overflow: 'hidden',
                  transition: 'all 0.3s ease',
                  cursor: 'pointer'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-4px)';
                  e.currentTarget.style.boxShadow = '0 15px 30px rgba(0, 0, 0, 0.15)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = 'none';
                }}>
                  <div style={{
                    padding: '1.5rem',
                    borderBottom: '1px solid rgba(0, 0, 0, 0.1)'
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
                      <div style={{ fontSize: '2.5rem' }}>{getDeviceIcon(device.type)}</div>
                      <div style={{ flex: 1 }}>
                        <h3 style={{ margin: '0 0 0.3rem 0', fontSize: '1.2rem', color: '#333' }}>
                          {device.name}
                        </h3>
                        <p style={{ margin: 0, color: '#666', fontSize: '0.9rem' }}>
                          {device.user} • {device.ip}
                        </p>
                      </div>
                      <div style={{
                        background: device.status === 'online' ? '#28a745' : '#6c757d',
                        color: 'white',
                        padding: '0.4rem 0.8rem',
                        borderRadius: '20px',
                        fontSize: '0.75rem',
                        fontWeight: '600',
                        textTransform: 'uppercase',
                        animation: device.status === 'online' ? 'pulse 2s infinite' : 'none'
                      }}>
                        {device.status}
                      </div>
                    </div>
                    
                    {/* Device Stats */}
                    <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
                      <div style={{
                        background: 'rgba(255, 255, 255, 0.7)',
                        padding: '0.75rem',
                        borderRadius: '12px',
                        flex: 1,
                        textAlign: 'center'
                      }}>
                        <div style={{ fontSize: '0.8rem', color: '#666', marginBottom: '0.25rem' }}>
                          Data Usage
                        </div>
                        <div style={{ fontSize: '1.1rem', fontWeight: '700', color: '#6a5acd' }}>
                          {device.dataUsage} GB
                        </div>
                      </div>
                      {device.battery && (
                        <div style={{
                          background: 'rgba(255, 255, 255, 0.7)',
                          padding: '0.75rem',
                          borderRadius: '12px',
                          flex: 1,
                          textAlign: 'center'
                        }}>
                          <div style={{ fontSize: '0.8rem', color: '#666', marginBottom: '0.25rem' }}>
                            Battery
                          </div>
                          <div style={{ fontSize: '1.1rem', fontWeight: '700', color: device.battery > 50 ? '#28a745' : '#dc3545' }}>
                            {device.battery}%
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                  
                  {/* Device Controls */}
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 0 }}>
                    <button
                      onClick={() => toggleDeviceBlock(device.id)}
                      style={{
                        padding: '1rem',
                        border: 'none',
                        background: device.blocked ? 'linear-gradient(135deg, #28a745, #20c997)' : 'linear-gradient(135deg, #dc3545, #c82333)',
                        color: 'white',
                        cursor: 'pointer',
                        fontWeight: '600',
                        fontSize: '0.9rem',
                        transition: 'all 0.2s ease',
                        borderRight: '1px solid rgba(255, 255, 255, 0.2)'
                      }}
                      onMouseEnter={(e) => e.target.style.filter = 'brightness(1.1)'}
                      onMouseLeave={(e) => e.target.style.filter = 'brightness(1)'}
                    >
                      {device.blocked ? '✅ Unblock' : '🚫 Block'}
                    </button>
                    <button
                      onClick={() => setModal({ type: 'schedule', data: device })}
                      style={{
                        padding: '1rem',
                        border: 'none',
                        background: 'linear-gradient(135deg, #6a5acd, #7b68ee)',
                        color: 'white',
                        cursor: 'pointer',
                        fontWeight: '600',
                        fontSize: '0.9rem',
                        transition: 'all 0.2s ease',
                        borderRight: '1px solid rgba(255, 255, 255, 0.2)'
                      }}
                      onMouseEnter={(e) => e.target.style.filter = 'brightness(1.1)'}
                      onMouseLeave={(e) => e.target.style.filter = 'brightness(1)'}
                    >
                      🗓️ Schedule
                    </button>
                    <button
                      style={{
                        padding: '1rem',
                        border: 'none',
                        background: 'linear-gradient(135deg, #17a2b8, #138496)',
                        color: 'white',
                        cursor: 'pointer',
                        fontWeight: '600',
                        fontSize: '0.9rem',
                        transition: 'all 0.2s ease'
                      }}
                      onMouseEnter={(e) => e.target.style.filter = 'brightness(1.1)'}
                      onMouseLeave={(e) => e.target.style.filter = 'brightness(1)'}
                    >
                      📊 Details
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right Sidebar */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            {/* Network Overview */}
            <div style={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              padding: '2rem',
              borderRadius: '24px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
              border: '1px solid rgba(255, 255, 255, 0.2)'
            }}>
              <h3 style={{ margin: '0 0 1.5rem 0', fontSize: '1.4rem', color: '#333', fontWeight: '700' }}>
                Network Usage
              </h3>
              <div style={{ width: '200px', height: '200px', margin: '0 auto 1rem' }}>
                <Doughnut 
                  data={networkChartData} 
                  options={{
                    maintainAspectRatio: false,
                    cutout: '70%',
                    plugins: {
                      legend: { display: false }
                    }
                  }}
                />
              </div>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '2rem', fontWeight: '700', color: '#6a5acd', marginBottom: '0.5rem' }}>
                  {mockNetworkStats.usedBandwidth}%
                </div>
                <div style={{ color: '#666', fontSize: '0.9rem', marginBottom: '1rem' }}>
                  Bandwidth Used
                </div>
                <div style={{ fontSize: '0.8rem', color: '#999' }}>
                  Peak: {mockNetworkStats.peakHours}
                </div>
                <div style={{ fontSize: '0.8rem', color: '#999' }}>
                  Speed: {mockNetworkStats.averageSpeed}
                </div>
              </div>
            </div>

            {/* Live Activity */}
            <div style={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              padding: '2rem',
              borderRadius: '24px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              flex: 1
            }}>
              <h3 style={{ margin: '0 0 1.5rem 0', fontSize: '1.4rem', color: '#333', fontWeight: '700' }}>
                Live Activity ({currentActivityLog.length} active)
              </h3>
              <div style={{ maxHeight: '300px', overflowY: 'auto' }}>
                {currentActivityLog.map((activity, index) => (
                  <div key={index} style={{
                    background: 'linear-gradient(135deg, #f8f9ff, #e8f5e8)',
                    padding: '1rem',
                    borderRadius: '16px',
                    marginBottom: '1rem',
                    border: '1px solid rgba(106, 90, 205, 0.1)',
                    transition: 'transform 0.2s ease'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.transform = 'translateX(4px)'}
                  onMouseLeave={(e) => e.currentTarget.style.transform = 'translateX(0)'}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem' }}>
                      <div style={{ fontSize: '1.5rem' }}>{getCategoryIcon(activity.category)}</div>
                      <div>
                        <div style={{ fontWeight: '600', color: '#333', fontSize: '0.9rem' }}>
                          {activity.deviceName}
                        </div>
                        <div style={{ color: '#666', fontSize: '0.8rem' }}>
                          {activity.category}
                        </div>
                      </div>
                    </div>
                    <div style={{
                      background: 'rgba(255, 255, 255, 0.8)',
                      padding: '0.75rem',
                      borderRadius: '12px',
                      fontSize: '0.85rem',
                      color: '#555',
                      wordBreak: 'break-all'
                    }}>
                      🌐 {activity.site}
                    </div>
                    <div style={{ 
                      textAlign: 'right', 
                      marginTop: '0.5rem', 
                      fontSize: '0.75rem', 
                      color: '#999' 
                    }}>
                      {formatTime(activity.timestamp)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Section - Analytics */}
        <div style={{
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(20px)',
          padding: '2rem',
          borderRadius: '24px',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
          border: '1px solid rgba(255, 255, 255, 0.2)'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '2rem'
          }}>
            <h2 style={{ margin: 0, fontSize: '1.8rem', color: '#333', fontWeight: '700' }}>
              Usage Analytics
            </h2>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              {['7d', '15d', '30d'].map(filter => (
                <button
                  key={filter}
                  onClick={() => setUsageFilter(filter)}
                  style={{
                    background: usageFilter === filter 
                      ? 'linear-gradient(135deg, #6a5acd, #7b68ee)' 
                      : 'rgba(106, 90, 205, 0.1)',
                    color: usageFilter === filter ? 'white' : '#6a5acd',
                    border: 'none',
                    padding: '0.75rem 1.5rem',
                    borderRadius: '12px',
                    cursor: 'pointer',
                    fontWeight: '600',
                    fontSize: '0.9rem',
                    transition: 'all 0.2s ease'
                  }}
                  onMouseEnter={(e) => {
                    if (usageFilter !== filter) {
                      e.target.style.background = 'rgba(106, 90, 205, 0.2)';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (usageFilter !== filter) {
                      e.target.style.background = 'rgba(106, 90, 205, 0.1)';
                    }
                  }}
                >
                  Last {filter.replace('d', ' Days')}
                </button>
              ))}
            </div>
          </div>
          
          <div style={{ height: '400px', width: '100%' }}>
            <Bar 
              data={usageChartData} 
              options={{
                maintainAspectRatio: false,
                responsive: true,
                plugins: {
                  legend: {
                    display: false
                  },
                  tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: 'white',
                    bodyColor: 'white',
                    borderColor: 'rgba(106, 90, 205, 0.8)',
                    borderWidth: 1,
                    cornerRadius: 12,
                    displayColors: false
                  }
                },
                scales: {
                  x: {
                    grid: {
                      display: false
                    },
                    ticks: {
                      color: '#666',
                      font: {
                        weight: '600'
                      }
                    }
                  },
                  y: {
                    grid: {
                      color: 'rgba(0, 0, 0, 0.05)'
                    },
                    ticks: {
                      color: '#666',
                      callback: function(value) {
                        return value + 'h';
                      }
                    }
                  }
                }
              }}
            />
          </div>
        </div>

        {/* Quick Actions Floating Panel */}
        <div style={{
          position: 'fixed',
          bottom: '2rem',
          right: '2rem',
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(20px)',
          padding: '1rem',
          borderRadius: '20px',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.15)',
          border: '1px solid rgba(255, 255, 255, 0.2)',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.75rem',
          zIndex: 1000
        }}>
          <div style={{ 
            fontSize: '0.8rem', 
            color: '#666', 
            fontWeight: '600', 
            textAlign: 'center',
            marginBottom: '0.5rem'
          }}>
            Quick Actions
          </div>
          
          {[
            { icon: '🚫', label: 'Block All', color: '#dc3545' },
            { icon: '✅', label: 'Unblock All', color: '#28a745' },
            { icon: '⏰', label: 'Bedtime', color: '#6a5acd' },
            { icon: '📊', label: 'Reports', color: '#17a2b8' }
          ].map((action, index) => (
            <button
              key={index}
              style={{
                background: `${action.color}15`,
                border: `2px solid ${action.color}30`,
                color: action.color,
                padding: '0.75rem',
                borderRadius: '12px',
                cursor: 'pointer',
                fontWeight: '600',
                fontSize: '0.85rem',
                transition: 'all 0.2s ease',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                minWidth: '120px'
              }}
              onMouseEnter={(e) => {
                e.target.style.background = action.color;
                e.target.style.color = 'white';
                e.target.style.transform = 'translateY(-2px)';
              }}
              onMouseLeave={(e) => {
                e.target.style.background = `${action.color}15`;
                e.target.style.color = action.color;
                e.target.style.transform = 'translateY(0)';
              }}
            >
              <span style={{ fontSize: '1.2rem' }}>{action.icon}</span>
              {action.label}
            </button>
          ))}
        </div>
      </main>
    </div>
  );
};

export default Dashboard;