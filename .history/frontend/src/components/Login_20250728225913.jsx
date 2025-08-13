import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './Login.css';

// Reusable Google Icon SVG for the button
const GoogleIcon = () => (
  <svg version="1.1" xmlns="http://www.w3.org/2000/svg" width="18px" height="18px" viewBox="0 0 48 48">
    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path>
    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"></path>
    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
    <path fill="none" d="M0 0h48v48H0z"></path>
  </svg>
);

const Login = () => {
  // State to manage which form is active: 'login', 'register', or 'forgotPassword'
  const [mode, setMode] = useState('login');
  const [loginCredentials, setLoginCredentials] = useState({ username: '', password: '' });
  const [registerData, setRegisterData] = useState({
    name: '',
    email: '',
    phone: '',
    age: '',
    password: '',
    profileImage: null,
  });
  const [resetEmail, setResetEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  // Mock credentials for fallback testing
  const mockCredentials = { username: 'parent', password: '123' };

  const handleInputChange = (e, formSetter) => {
    const { name, value } = e.target;
    formSetter(prev => ({ ...prev, [name]: value }));
    if (error) setError('');
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setRegisterData(prev => ({ ...prev, profileImage: URL.createObjectURL(file) }));
    }
  };

  // --- FORM SUBMISSION HANDLERS ---

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const response = await axios.post('http://localhost:8080/api/auth/login', loginCredentials);
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('parentName', response.data.name || loginCredentials.username);
      navigate('/dashboard');
    } catch (apiError) {
      console.log('API failed, trying mock authentication...');
      if (loginCredentials.username === mockCredentials.username && loginCredentials.password === mockCredentials.password) {
        localStorage.setItem('token', 'mock_token_123');
        localStorage.setItem('parentName', loginCredentials.username);
        navigate('/dashboard');
      } else {
        setError('Invalid username or password.');
      }
    } finally {
      setLoading(false);
    }
  };

const handleRegister = async (e) => {
  e.preventDefault();
  setLoading(true);
  setError('');

  // Use FormData to send both the file and the JSON data
  const formData = new FormData();

  // The file input needs to be accessed via its ref
  if (fileInputRef.current.files[0]) {
    formData.append('profileImage', fileInputRef.current.files[0]);
  }

  // The rest of the data needs to be sent as a JSON string under a specific part name
  // This matches the @RequestPart("request") in the Spring Boot controller
  const requestData = {
    name: registerData.name,
    email: registerData.email,
    phone: registerData.phone,
    age: registerData.age,
    password: registerData.password
  };
  
  formData.append('request', new Blob([JSON.stringify(requestData)], { type: "application/json" }));

  try {
    // Make the POST request to the backend registration endpoint
    await axios.post('http://localhost:8080/api/auth/register', formData, {
      headers: {
        // The browser will set the correct Content-Type with boundary for multipart/form-data
        // So we don't need to set it manually here.
      },
    });

    alert('Registration successful! Please login.');
    setMode('login'); // Switch to the login form on success

  } catch (apiError) {
    const message = apiError.response?.data?.message || 'Registration failed. Please try again.';
    setError(message);
    console.error('Registration error:', apiError.response || apiError);
  } finally {
    setLoading(false);
  }
};

  const handlePasswordReset = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    // Placeholder for password reset logic
    console.log('Resetting password for:', resetEmail, 'with OTP:', otp);
    // Simulate API call
    setTimeout(() => {
      alert('Password reset instructions sent to your email.');
      setMode('login'); // Switch back to login
      setLoading(false);
    }, 1500);
  };

  const handleGoogleSignUp = () => {
    // Placeholder for Google Sign-Up logic
    alert("Redirecting to Google Sign-Up...");
  };


  // --- FORM RENDER FUNCTIONS ---

  const renderLoginForm = () => (
    <form onSubmit={handleLogin} className="login-form">
      <div className="form-group">
        <label htmlFor="username">Parent Username</label>
        <input id="username" type="text" name="username" placeholder="e.g., parent" value={loginCredentials.username} onChange={(e) => handleInputChange(e, setLoginCredentials)} required disabled={loading} />
      </div>
      <div className="form-group">
        <label htmlFor="password">Password</label>
        <input id="password" type="password" name="password" placeholder="e.g., 123" value={loginCredentials.password} onChange={(e) => handleInputChange(e, setLoginCredentials)} required disabled={loading} />
      </div>
      {error && <div className="error-message">{error}</div>}
      <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
        {loading ? 'Logging in...' : 'Login to Dashboard'}
      </button>
      <div className="form-switcher">
        <a href="#" onClick={(e) => { e.preventDefault(); setMode('forgotPassword'); setError(''); }}>Forgot Password?</a>
        <span> | </span>
        <a href="#" onClick={(e) => { e.preventDefault(); setMode('register'); setError(''); }}>No account? <strong>Register</strong></a>
      </div>
    </form>
  );

  const renderRegisterForm = () => (
    <form onSubmit={handleRegister} className="login-form">
      <div className="form-group profile-image-group" onClick={() => fileInputRef.current.click()}>
        <input type="file" ref={fileInputRef} onChange={handleFileChange} accept="image/*" style={{ display: 'none' }} />
        <div className="profile-image-preview">
          {registerData.profileImage ? <img src={registerData.profileImage} alt="Profile Preview" /> : <span>📷<br/>Upload</span>}
        </div>
      </div>
      <div className="form-group">
        <label>Full Name</label>
        <input type="text" name="name" placeholder="Enter your full name" value={registerData.name} onChange={(e) => handleInputChange(e, setRegisterData)} required disabled={loading} />
      </div>
      <div className="form-group">
        <label>Email Address</label>
        <input type="email" name="email" placeholder="Enter your email" value={registerData.email} onChange={(e) => handleInputChange(e, setRegisterData)} required disabled={loading} />
      </div>
      <div className="form-group">
        <label>Phone Number</label>
        <input type="tel" name="phone" placeholder="Enter your phone number" value={registerData.phone} onChange={(e) => handleInputChange(e, setRegisterData)} required disabled={loading} />
      </div>
       <div className="form-group">
        <label>Age</label>
        <input type="number" name="age" placeholder="Enter your age" value={registerData.age} onChange={(e) => handleInputChange(e, setRegisterData)} required disabled={loading} />
      </div>
      <div className="form-group">
        <label>Password</label>
        <input type="password" name="password" placeholder="Create a strong password" value={registerData.password} onChange={(e) => handleInputChange(e, setRegisterData)} required disabled={loading} />
      </div>
      <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
        {loading ? 'Registering...' : 'Create Account'}
      </button>
       <button type="button" className="google-btn" onClick={handleGoogleSignUp} disabled={loading}>
        <GoogleIcon />
        <span>Sign up with Google</span>
      </button>
      <div className="form-switcher">
        <a href="#" onClick={(e) => { e.preventDefault(); setMode('login'); setError(''); }}>Already have an account? <strong>Login</strong></a>
      </div>
    </form>
  );

  const renderForgotPasswordForm = () => (
    <form onSubmit={handlePasswordReset} className="login-form">
      <p className="reset-info">Enter your email to receive a One-Time Password (OTP).</p>
      <div className="form-group">
        <label htmlFor="reset-email">Email Address</label>
        <input id="reset-email" type="email" placeholder="Enter your registered email" value={resetEmail} onChange={(e) => setResetEmail(e.target.value)} required disabled={loading} />
      </div>
      <div className="form-group">
        <label htmlFor="otp">OTP</label>
        <input id="otp" type="text" placeholder="Enter the OTP from your email" value={otp} onChange={(e) => setOtp(e.target.value)} required disabled={loading} />
      </div>
      <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
        {loading ? 'Verifying...' : 'Reset Password'}
      </button>
      <div className="form-switcher">
        <a href="#" onClick={(e) => { e.preventDefault(); setMode('login'); setError(''); }}>Back to <strong>Login</strong></a>
      </div>
    </form>
  );

  return (
    <div className="login-container">
      <div className="login-background">
        <div className="login-card">
          <div className="login-header">
            <div className="logo">🛡️</div>
            <h1>{mode === 'login' ? 'NetGuard Login' : mode === 'register' ? 'Create Account' : 'Reset Password'}</h1>
            <p>Smart Parental Internet Control</p>
          </div>
          
          {mode === 'login' && (
             <div className="demo-info">
                <strong>Demo Mode:</strong> username: "parent" | password: "123"
             </div>
          )}

          {mode === 'login' && renderLoginForm()}
          {mode === 'register' && renderRegisterForm()}
          {mode === 'forgotPassword' && renderForgotPasswordForm()}

          <div className="login-footer">
            <p>Secure parental control for your home network</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;