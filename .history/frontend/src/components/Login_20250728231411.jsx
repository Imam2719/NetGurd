import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './Login.css'; // Assuming this CSS file exists for styling

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
  // State for login form credentials
  const [loginCredentials, setLoginCredentials] = useState({ username: '', password: '' });
  // State for registration form data
  const [registerData, setRegisterData] = useState({
    name: '',
    email: '',
    phone: '',
    age: '',
    password: '',
    profileImageFile: null, // Stores the actual File object
  });
  // State for profile image preview URL
  const [profileImagePreviewUrl, setProfileImagePreviewUrl] = useState(null);
  // State for forgot password flow
  const [forgotPasswordEmail, setForgotPasswordEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  // State to control steps within the forgot password flow: 'emailInput', 'otpInput', 'newPasswordInput'
  const [forgotPasswordStep, setForgotPasswordStep] = useState('emailInput');

  const [loading, setLoading] = useState(false); // Loading state for API calls
  const [error, setError] = useState(''); // Error message state
  const [message, setMessage] = useState(''); // General success/info message state

  const navigate = useNavigate(); // Hook for navigation
  const fileInputRef = useRef(null); // Ref for file input element

  // Mock credentials for fallback testing (as per original code)
  const mockCredentials = { username: 'parent', password: '123' };

  // Handles input changes for form fields
  const handleInputChange = (e, formSetter) => {
    const { name, value } = e.target;
    formSetter(prev => ({ ...prev, [name]: value }));
    // Clear error/message when user starts typing
    if (error) setError('');
    if (message) setMessage('');
  };

  // Handles file input change for profile image
  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setRegisterData(prev => ({ ...prev, profileImageFile: file })); // Store the actual file
      setProfileImagePreviewUrl(URL.createObjectURL(file)); // Create URL for preview
    }
  };

  // --- FORM SUBMISSION HANDLERS ---

  // Handles user login
  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    try {
      const response = await axios.post('http://localhost:8080/api/auth/login', loginCredentials);
      localStorage.setItem('token', response.data.data.token); // Access token from response.data.data
      localStorage.setItem('parentName', response.data.data.name || loginCredentials.username); // Access name from response.data.data
      setMessage(response.data.message || 'Login successful!');
      navigate('/dashboard');
    } catch (apiError) {
      console.error('API login failed:', apiError.response || apiError);
      // Fallback to mock authentication if API fails
      if (loginCredentials.username === mockCredentials.username && loginCredentials.password === mockCredentials.password) {
        localStorage.setItem('token', 'mock_token_123');
        localStorage.setItem('parentName', loginCredentials.username);
        setMessage('Mock login successful!');
        navigate('/dashboard');
      } else {
        setError(apiError.response?.data?.message || 'Invalid username or password.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Handles new user registration
  const handleRegister = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    const formData = new FormData();

    // Append the profile image file if selected
    if (registerData.profileImageFile) {
      formData.append('profileImage', registerData.profileImageFile);
    }

    // Create a Blob for the JSON request data
    const requestData = {
      name: registerData.name,
      email: registerData.email,
      phone: registerData.phone,
      // Ensure age is sent as a number or null to match backend Integer type
      age: registerData.age ? parseInt(registerData.age, 10) : null,
      password: registerData.password
    };

    // Append the JSON data as a Blob with content type 'application/json'
    // This matches the @RequestPart("request") in the Spring Boot controller
    formData.append('request', new Blob([JSON.stringify(requestData)], { type: "application/json" }));

    try {
      const response = await axios.post('http://localhost:8080/api/auth/register', formData, {
        headers: {
          // Axios automatically sets 'Content-Type': 'multipart/form-data' with the correct boundary
          // when a FormData object is passed as the data. Do not set it manually.
        },
      });

      setMessage(response.data.message || 'Registration successful! Please login.');
      setMode('login'); // Switch to the login form on success
      setRegisterData({ // Clear registration form
        name: '', email: '', phone: '', age: '', password: '', profileImageFile: null,
      });
      setProfileImagePreviewUrl(null); // Clear image preview
    } catch (apiError) {
      const msg = apiError.response?.data?.message || 'Registration failed. Please try again.';
      setError(msg);
      console.error('Registration error:', apiError.response || apiError);
    } finally {
      setLoading(false);
    }
  };

  // Handles sending forgot password request (OTP generation)
  const handleForgotPasswordRequest = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    try {
      const response = await axios.post('http://localhost:8080/api/auth/forgot-password', { email: forgotPasswordEmail });
      setMessage(response.data.message || 'OTP sent to your email. Please check your inbox.');
      setForgotPasswordStep('otpInput'); // Move to OTP input step
    } catch (apiError) {
      const msg = apiError.response?.data?.message || 'Failed to send OTP. Please check your email and try again.';
      setError(msg);
      console.error('Forgot password request error:', apiError.response || apiError);
    } finally {
      setLoading(false);
    }
  };

  // Handles resetting the password with OTP
  const handleResetPassword = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    if (newPassword !== confirmNewPassword) {
      setError('New password and confirm password do not match.');
      setLoading(false);
      return;
    }

    try {
      const response = await axios.post('http://localhost:8080/api/auth/reset-password', {
        email: forgotPasswordEmail,
        otp: otp,
        newPassword: newPassword,
      });
      setMessage(response.data.message || 'Password reset successfully! You can now log in with your new password.');
      setMode('login'); // Switch back to login form
      setForgotPasswordEmail(''); // Clear reset form states
      setOtp('');
      setNewPassword('');
      setConfirmNewPassword('');
      setForgotPasswordStep('emailInput'); // Reset forgot password flow step
    } catch (apiError) {
      const msg = apiError.response?.data?.message || 'Failed to reset password. Please check your OTP or try again.';
      setError(msg);
      console.error('Reset password error:', apiError.response || apiError);
    } finally {
      setLoading(false);
    }
  };

  // Placeholder for Google Sign-Up logic
  const handleGoogleSignUp = () => {
    alert("Redirecting to Google Sign-Up...");
  };


  // --- FORM RENDER FUNCTIONS ---

  // Renders the login form
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
      {message && <div className="success-message">{message}</div>}
      <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
        {loading ? 'Logging in...' : 'Login to Dashboard'}
      </button>
      <div className="form-switcher">
        <a href="#" onClick={(e) => { e.preventDefault(); setMode('forgotPassword'); setError(''); setMessage(''); setForgotPasswordEmail(''); setForgotPasswordStep('emailInput'); }}>Forgot Password?</a>
        <span> | </span>
        <a href="#" onClick={(e) => { e.preventDefault(); setMode('register'); setError(''); setMessage(''); }}>No account? <strong>Register</strong></a>
      </div>
    </form>
  );

  // Renders the registration form
  const renderRegisterForm = () => (
    <form onSubmit={handleRegister} className="login-form">
      <div className="form-group profile-image-group" onClick={() => fileInputRef.current.click()}>
        <input type="file" ref={fileInputRef} onChange={handleFileChange} accept="image/*" style={{ display: 'none' }} disabled={loading} />
        <div className="profile-image-preview">
          {profileImagePreviewUrl ? <img src={profileImagePreviewUrl} alt="Profile Preview" /> : <span>📷<br />Upload</span>}
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
      {error && <div className="error-message">{error}</div>}
      {message && <div className="success-message">{message}</div>}
      <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
        {loading ? 'Registering...' : 'Create Account'}
      </button>
      <button type="button" className="google-btn" onClick={handleGoogleSignUp} disabled={loading}>
        <GoogleIcon />
        <span>Sign up with Google</span>
      </button>
      <div className="form-switcher">
        <a href="#" onClick={(e) => { e.preventDefault(); setMode('login'); setError(''); setMessage(''); }}>Already have an account? <strong>Login</strong></a>
      </div>
    </form>
  );

  // Renders the forgot password form based on the current step
  const renderForgotPasswordForm = () => {
    switch (forgotPasswordStep) {
      case 'emailInput':
        return (
          <form onSubmit={handleForgotPasswordRequest} className="login-form">
            <p className="reset-info">Enter your email to receive a One-Time Password (OTP).</p>
            <div className="form-group">
              <label htmlFor="reset-email">Email Address</label>
              <input id="reset-email" type="email" placeholder="Enter your registered email" value={forgotPasswordEmail} onChange={(e) => { setForgotPasswordEmail(e.target.value); setError(''); setMessage(''); }} required disabled={loading} />
            </div>
            {error && <div className="error-message">{error}</div>}
            {message && <div className="success-message">{message}</div>}
            <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
              {loading ? 'Sending OTP...' : 'Send OTP'}
            </button>
            <div className="form-switcher">
              <a href="#" onClick={(e) => { e.preventDefault(); setMode('login'); setError(''); setMessage(''); setForgotPasswordEmail(''); setForgotPasswordStep('emailInput'); }}>Back to <strong>Login</strong></a>
            </div>
          </form>
        );
      case 'otpInput':
        return (
          <form onSubmit={(e) => { e.preventDefault(); setForgotPasswordStep('newPasswordInput'); }} className="login-form">
            <p className="reset-info">Enter the OTP sent to {forgotPasswordEmail} and your new password.</p>
            <div className="form-group">
              <label htmlFor="otp">OTP</label>
              <input id="otp" type="text" placeholder="Enter the OTP from your email" value={otp} onChange={(e) => { setOtp(e.target.value); setError(''); setMessage(''); }} required disabled={loading} />
            </div>
            {error && <div className="error-message">{error}</div>}
            {message && <div className="success-message">{message}</div>}
            <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
              {loading ? 'Verifying OTP...' : 'Verify OTP & Set New Password'}
            </button>
            <div className="form-switcher">
              <a href="#" onClick={(e) => { e.preventDefault(); setForgotPasswordStep('emailInput'); setError(''); setMessage(''); }}>Back to Email Input</a>
              <span> | </span>
              <a href="#" onClick={(e) => { e.preventDefault(); setMode('login'); setError(''); setMessage(''); setForgotPasswordEmail(''); setForgotPasswordStep('emailInput'); }}>Back to <strong>Login</strong></a>
            </div>
          </form>
        );
      case 'newPasswordInput':
        return (
          <form onSubmit={handleResetPassword} className="login-form">
            <p className="reset-info">Enter your new password.</p>
            <div className="form-group">
              <label htmlFor="new-password">New Password</label>
              <input id="new-password" type="password" placeholder="Enter your new password" value={newPassword} onChange={(e) => { setNewPassword(e.target.value); setError(''); setMessage(''); }} required disabled={loading} />
            </div>
            <div className="form-group">
              <label htmlFor="confirm-new-password">Confirm New Password</label>
              <input id="confirm-new-password" type="password" placeholder="Confirm your new password" value={confirmNewPassword} onChange={(e) => { setConfirmNewPassword(e.target.value); setError(''); setMessage(''); }} required disabled={loading} />
            </div>
            {error && <div className="error-message">{error}</div>}
            {message && <div className="success-message">{message}</div>}
            <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
              {loading ? 'Resetting Password...' : 'Reset Password'}
            </button>
            <div className="form-switcher">
              <a href="#" onClick={(e) => { e.preventDefault(); setForgotPasswordStep('otpInput'); setError(''); setMessage(''); }}>Back to OTP Input</a>
              <span> | </span>
              <a href="#" onClick={(e) => { e.preventDefault(); setMode('login'); setError(''); setMessage(''); setForgotPasswordEmail(''); setForgotPasswordStep('emailInput'); }}>Back to <strong>Login</strong></a>
            </div>
          </form>
        );
      default:
        return null;
    }
  };

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