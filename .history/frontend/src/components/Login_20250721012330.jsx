import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './Login.css';

const Login = () => {
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  // Mock credentials for testing
  const mockCredentials = {
    username: 'parent',
    password: '123'
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // Try real API first
      const response = await axios.post('http://localhost:8080/api/auth/login', credentials);
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('parentName', response.data.name || credentials.username);
      navigate('/dashboard');
    } catch (error) {
      // Fallback to mock authentication if API fails
      console.log('API failed, trying mock authentication...');

      if (credentials.username === mockCredentials.username &&
          credentials.password === mockCredentials.password) {
        // Mock successful login
        localStorage.setItem('token', 'mock_token_123');
        localStorage.setItem('parentName', credentials.username);
        navigate('/dashboard');
      } else {
        setError('Invalid username or password. Try: username="parent", password="netguard123"');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field, value) => {
    setCredentials({ ...credentials, [field]: value });
    if (error) setError(''); // Clear error when user types
  };

  return (
    <div className="login-container">
      <div className="login-background">
        <div className="login-card">
          <div className="login-header">
            <div className="logo">
              🛡️
            </div>
            <h1>NetGuard</h1>
            <p>Smart Parental Internet Control</p>
          </div>

          {/* Demo credentials info */}
          <div style={{
            background: '#e6f3ff',
            padding: '10px',
            borderRadius: '6px',
            marginBottom: '20px',
            fontSize: '12px',
            color: '#0066cc'
          }}>
            <strong>Demo Mode:</strong> username: "parent" | password: "123"
          </div>

          <form onSubmit={handleLogin} className="login-form">
            <div className="form-group">
              <label htmlFor="username">Parent Username</label>
              <input
                id="username"
                type="text"
                placeholder="Enter your username"
                value={credentials.username}
                onChange={(e) => handleInputChange('username', e.target.value)}
                required
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                placeholder="Enter your password"
                value={credentials.password}
                onChange={(e) => handleInputChange('password', e.target.value)}
                required
                disabled={loading}
              />
            </div>

            {error && <div className="error-message">{error}</div>}

            <button
              type="submit"
              className={`login-btn ${loading ? 'loading' : ''}`}
              disabled={loading}
            >
              {loading ? 'Logging in...' : 'Login to Dashboard'}
            </button>
          </form>

          <div className="login-footer">
            <p>Secure parental control for your home network</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;