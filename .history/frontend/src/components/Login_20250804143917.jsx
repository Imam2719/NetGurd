import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import './Login.css';

// Base URL for your Spring Boot backend API
const API_BASE_URL = 'http://localhost:8080/api/auth';

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

// Terms and Conditions Modal Component
const TermsModal = ({ isOpen, onClose, type }) => {
  if (!isOpen) return null;

  const termsContent = {
    terms: {
      title: "Terms of Service",
      content: (
        <>
          <h3>1. Acceptance of Terms</h3>
          <p>By accessing and using NetGuard, you accept and agree to be bound by the terms and provision of this agreement.</p>
          
          <h3>2. Service Description</h3>
          <p>NetGuard is a smart parental internet control system that helps parents manage their children's internet usage and screen time.</p>
          
          <h3>3. User Responsibilities</h3>
          <ul>
            <li>Provide accurate and complete information during registration</li>
            <li>Maintain the confidentiality of your account credentials</li>
            <li>Use the service only for lawful parental control purposes</li>
            <li>Respect the privacy and rights of all family members</li>
          </ul>
          
          <h3>4. Service Limitations</h3>
          <p>While NetGuard strives to provide reliable service, we cannot guarantee 100% effectiveness in all network environments or against all bypass attempts.</p>
          
          <h3>5. Account Termination</h3>
          <p>We reserve the right to terminate accounts that violate these terms or engage in abusive behavior.</p>
          
          <h3>6. Updates to Terms</h3>
          <p>These terms may be updated periodically. Continued use of the service constitutes acceptance of any changes.</p>
        </>
      )
    },
    privacy: {
      title: "Privacy Policy",
      content: (
        <>
          <h3>1. Information We Collect</h3>
          <p>We collect information you provide directly, such as account details and device information necessary for parental controls.</p>
          
          <h3>2. How We Use Information</h3>
          <ul>
            <li>Provide and maintain our parental control services</li>
            <li>Monitor and analyze usage patterns for service improvement</li>
            <li>Communicate with you about your account and service updates</li>
            <li>Ensure the security and integrity of our service</li>
          </ul>
          
          <h3>3. Information Sharing</h3>
          <p>We do not sell, trade, or share your personal information with third parties except as described in this policy or with your consent.</p>
          
          <h3>4. Data Security</h3>
          <p>We implement appropriate security measures to protect your personal information against unauthorized access, alteration, disclosure, or destruction.</p>
          
          <h3>5. Children's Privacy</h3>
          <p>Our service is designed for parents to monitor their minor children's internet usage. We collect minimal data necessary for this purpose.</p>
          
          <h3>6. Your Rights</h3>
          <p>You have the right to access, update, or delete your personal information. Contact us for assistance with these requests.</p>
          
          <h3>7. Contact Information</h3>
          <p>For privacy-related questions, contact us at privacy@netguard.com</p>
        </>
      )
    }
  };

  const currentContent = termsContent[type] || termsContent.terms;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{currentContent.title}</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          {currentContent.content}
        </div>
      </div>
    </div>
  );
};

const Login = () => {
  // Theme management
  const [darkMode, setDarkMode] = useState(() => {
    const saved = localStorage.getItem('netguard-theme');
    return saved ? saved === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
  });

  // Modal states
  const [modalState, setModalState] = useState({ isOpen: false, type: 'terms' });

  // Password visibility states
  const [passwordVisibility, setPasswordVisibility] = useState({
    password: false,
    newPassword: false,
    confirmPassword: false
  });

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
    acceptTerms: false,
    acceptPrivacy: false,
    profileImageFile: null,
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

  // Theme effect
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', darkMode ? 'dark' : 'light');
    localStorage.setItem('netguard-theme', darkMode ? 'dark' : 'light');
  }, [darkMode]);

  // Toggle theme
  const toggleTheme = () => {
    setDarkMode(prev => !prev);
  };

  // Toggle password visibility
  const togglePasswordVisibility = (field) => {
    setPasswordVisibility(prev => ({
      ...prev,
      [field]: !prev[field]
    }));
  };

  // Open modal
  const openModal = (type) => {
    setModalState({ isOpen: true, type });
  };

  // Close modal
  const closeModal = () => {
    setModalState({ isOpen: false, type: 'terms' });
  };

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
      setRegisterData(prev => ({ ...prev, profileImageFile: file }));
      setProfileImagePreviewUrl(URL.createObjectURL(file));
    }
  };

  // --- FORM SUBMISSION HANDLERS ---

  /**
   * Handles user login by sending credentials to the backend.
   */
  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    try {
      const response = await axios.post(`${API_BASE_URL}/login`, loginCredentials);

      if (response.data.success && response.data.data) {
        localStorage.setItem('token', response.data.data.token);
        localStorage.setItem('parentName', response.data.data.name || loginCredentials.username);
        setMessage(response.data.message || 'Login successful!');
        navigate('/dashboard');
      } else {
        setError(response.data.message || 'Login failed. Please try again.');
      }
    } catch (apiError) {
      console.error('API login failed:', apiError.response || apiError);
      setError(apiError.response?.data?.message || 'Login failed. Invalid username or password.');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Enhanced registration handler with comprehensive validation
   */
  const handleRegister = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    try {
      // Client-side validation
      if (!registerData.name || !registerData.email || !registerData.phone || !registerData.age || !registerData.password) {
        setError('Please fill in all required fields.');
        setLoading(false);
        return;
      }

      if (!registerData.acceptTerms) {
        setError('You must accept the Terms of Service to create an account.');
        setLoading(false);
        return;
      }

      if (!registerData.acceptPrivacy) {
        setError('You must accept the Privacy Policy to create an account.');
        setLoading(false);
        return;
      }

      // Age validation
      const ageNumber = parseInt(registerData.age, 10);
      if (isNaN(ageNumber) || ageNumber < 18 || ageNumber > 120) {
        setError('Please enter a valid age between 18 and 120.');
        setLoading(false);
        return;
      }

      // Password strength validation
      if (registerData.password.length < 6) {
        setError('Password must be at least 6 characters long.');
        setLoading(false);
        return;
      }

      // Email format validation
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(registerData.email)) {
        setError('Please enter a valid email address.');
        setLoading(false);
        return;
      }

      const formData = new FormData();

      if (registerData.profileImageFile) {
        // File size validation (5MB)
        if (registerData.profileImageFile.size > 5 * 1024 * 1024) {
          setError('Profile image must be smaller than 5MB.');
          setLoading(false);
          return;
        }

        // File type validation
        const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
        if (!allowedTypes.includes(registerData.profileImageFile.type)) {
          setError('Profile image must be a valid image file (JPEG, PNG, GIF, or WebP).');
          setLoading(false);
          return;
        }

        formData.append('profileImage', registerData.profileImageFile);
      }

      const requestData = {
        name: registerData.name.trim(),
        email: registerData.email.trim().toLowerCase(),
        phone: registerData.phone.trim(),
        age: ageNumber,
        password: registerData.password,
        acceptTerms: registerData.acceptTerms,
        acceptPrivacy: registerData.acceptPrivacy
      };

      formData.append('request', new Blob([JSON.stringify(requestData)], {
        type: "application/json"
      }));

      const response = await axios.post(`${API_BASE_URL}/register`, formData, {
        timeout: 30000
      });

      if (response.data && response.data.success) {
        const successMessage = response.data.message || 'Registration successful! Please login with your credentials.';
        setMessage(successMessage);
        setMode('login');
        
        // Clear form
        setRegisterData({
          name: '',
          email: '',
          phone: '',
          age: '',
          password: '',
          acceptTerms: false,
          acceptPrivacy: false,
          profileImageFile: null
        });
        setProfileImagePreviewUrl(null);
        setError('');
      } else {
        setError(response.data?.message || 'Registration failed. Please check your information and try again.');
      }

    } catch (apiError) {
      console.error('Registration error:', apiError);

      if (apiError.response) {
        if (apiError.response.status === 400) {
          let errorMessage = 'Registration failed due to invalid data.';
          
          if (apiError.response.data) {
            if (apiError.response.data.message) {
              errorMessage = apiError.response.data.message;
            } else if (apiError.response.data.error) {
              errorMessage = apiError.response.data.error;
            } else if (apiError.response.data.errors && Array.isArray(apiError.response.data.errors)) {
              errorMessage = apiError.response.data.errors.join(', ');
            }
          }
          setError(errorMessage);
        } else if (apiError.response.status === 409) {
          setError('An account with this email or phone number already exists.');
        } else if (apiError.response.status >= 500) {
          setError('Server error occurred. Please try again later.');
        } else {
          setError(apiError.response.data?.message || `Registration failed (Error ${apiError.response.status})`);
        }
      } else if (apiError.request) {
        setError('Network error. Please check your internet connection and try again.');
      } else if (apiError.code === 'ECONNABORTED') {
        setError('Request timed out. Please try again.');
      } else {
        setError('An unexpected error occurred. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handles forgot password request
   */
  const handleForgotPasswordRequest = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    try {
      const response = await axios.post(`${API_BASE_URL}/forgot-password`, { email: forgotPasswordEmail });

      if (response.data.success) {
        setMessage(response.data.message || 'OTP sent to your email. Please check your inbox.');
        setForgotPasswordStep('otpInput');
      } else {
        setError(response.data.message || 'Failed to send OTP. Please check your email and try again.');
      }
    } catch (apiError) {
      console.error('Forgot password request error:', apiError.response || apiError);
      const msg = apiError.response?.data?.message || 'Failed to send OTP. Please check your email and try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handles password reset
   */
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
      const response = await axios.post(`${API_BASE_URL}/reset-password`, {
        email: forgotPasswordEmail,
        otp: otp,
        newPassword: newPassword,
      });

      if (response.data.success) {
        setMessage(response.data.message || 'Password reset successfully! You can now log in with your new password.');
        setMode('login');
        setForgotPasswordEmail('');
        setOtp('');
        setNewPassword('');
        setConfirmNewPassword('');
        setForgotPasswordStep('emailInput');
      } else {
        setError(response.data.message || 'Failed to reset password. Please check your OTP or try again.');
      }
    } catch (apiError) {
      console.error('Reset password error:', apiError.response || apiError);
      const msg = apiError.response?.data?.message || 'Failed to reset password. Please check your OTP or try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignUp = () => {
    alert("Google Sign-Up integration coming soon!");
  };

  // Enhanced Password Input Component
  const PasswordInput = ({ 
    id, 
    name, 
    placeholder, 
    value, 
    onChange, 
    required = false, 
    disabled = false,
    visibilityField 
  }) => (
    <div className="password-input-container">
      <input
        id={id}
        type={passwordVisibility[visibilityField] ? 'text' : 'password'}
        name={name}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
        autoComplete={name === 'password' ? 'current-password' : 'new-password'}
        style={{ paddingRight: '50px' }}
      />
      <button
        type="button"
        className="password-toggle"
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
          togglePasswordVisibility(visibilityField);
        }}
        disabled={disabled}
        tabIndex={-1}
        aria-label={passwordVisibility[visibilityField] ? 'Hide password' : 'Show password'}
      >
        {passwordVisibility[visibilityField] ? '🙈' : '👁️'}
      </button>
    </div>
  );

  // --- FORM RENDER FUNCTIONS ---

  const renderLoginForm = () => (
    <form onSubmit={handleLogin} className="login-form">
      <div className="form-group">
        <label htmlFor="username">Parent Username</label>
        <input
          id="username"
          type="text"
          name="username"
          placeholder="Enter your email"
          value={loginCredentials.username}
          onChange={(e) => handleInputChange(e, setLoginCredentials)}
          required
          disabled={loading}
        />
      </div>
      <div className="form-group">
        <label htmlFor="password">Password</label>
        <PasswordInput
          id="password"
          name="password"
          placeholder="Enter your password"
          value={loginCredentials.password}
          onChange={(e) => handleInputChange(e, setLoginCredentials)}
          required={true}
          disabled={loading}
          visibilityField="password"
        />
      </div>
      {error && <div className="error-message">{error}</div>}
      {message && <div className="success-message">{message}</div>}
      <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
        {loading ? 'Logging in...' : 'Login to Dashboard'}
      </button>
      <div className="form-switcher">
        <a href="#" onClick={(e) => { 
          e.preventDefault(); 
          setMode('forgotPassword'); 
          setError(''); 
          setMessage(''); 
          setForgotPasswordEmail(''); 
          setForgotPasswordStep('emailInput'); 
        }}>Forgot Password?</a>
        <span> | </span>
        <a href="#" onClick={(e) => { 
          e.preventDefault(); 
          setMode('register'); 
          setError(''); 
          setMessage(''); 
        }}>No account? <strong>Register</strong></a>
      </div>
    </form>
  );

  const renderRegisterForm = () => (
    <form onSubmit={handleRegister} className="login-form">
      <div className="form-group profile-image-group" onClick={() => fileInputRef.current?.click()}>
        <input 
          type="file" 
          ref={fileInputRef} 
          onChange={handleFileChange} 
          accept="image/*" 
          style={{ display: 'none' }} 
          disabled={loading} 
        />
        <div className="profile-image-preview">
          {profileImagePreviewUrl ? 
            <img src={profileImagePreviewUrl} alt="Profile Preview" /> : 
            <span>📷<br />Upload</span>
          }
        </div>
      </div>
      
      <div className="form-group">
        <label>Full Name</label>
        <input 
          type="text" 
          name="name" 
          placeholder="Enter your full name" 
          value={registerData.name} 
          onChange={(e) => handleInputChange(e, setRegisterData)} 
          required 
          disabled={loading} 
        />
      </div>
      
      <div className="form-group">
        <label>Email Address</label>
        <input 
          type="email" 
          name="email" 
          placeholder="Enter your email" 
          value={registerData.email} 
          onChange={(e) => handleInputChange(e, setRegisterData)} 
          required 
          disabled={loading} 
        />
      </div>
      
      <div className="form-group">
        <label>Phone Number</label>
        <input 
          type="tel" 
          name="phone" 
          placeholder="Enter your phone number" 
          value={registerData.phone} 
          onChange={(e) => handleInputChange(e, setRegisterData)} 
          required 
          disabled={loading} 
        />
      </div>
      
      <div className="form-group">
        <label>Age</label>
        <input 
          type="number" 
          name="age" 
          placeholder="Enter your age" 
          value={registerData.age} 
          onChange={(e) => handleInputChange(e, setRegisterData)} 
          required 
          disabled={loading}
          min="18"
          max="120" 
        />
      </div>
      
      <div className="form-group">
        <label>Password</label>
        <PasswordInput
          id="register-password"
          name="password"
          placeholder="Create a strong password"
          value={registerData.password}
          onChange={(e) => handleInputChange(e, setRegisterData)}
          required={true}
          disabled={loading}
          visibilityField="password"
        />
      </div>

      <div className="checkbox-group">
        <input
          type="checkbox"
          id="acceptTerms"
          name="acceptTerms"
          checked={registerData.acceptTerms}
          onChange={(e) => setRegisterData(prev => ({ ...prev, acceptTerms: e.target.checked }))}
          required
          disabled={loading}
        />
        <label htmlFor="acceptTerms">
          I accept the <a href="#" onClick={(e) => { e.preventDefault(); openModal('terms'); }}>Terms of Service</a>
        </label>
      </div>

      <div className="checkbox-group">
        <input
          type="checkbox"
          id="acceptPrivacy"
          name="acceptPrivacy"
          checked={registerData.acceptPrivacy}
          onChange={(e) => setRegisterData(prev => ({ ...prev, acceptPrivacy: e.target.checked }))}
          required
          disabled={loading}
        />
        <label htmlFor="acceptPrivacy">
          I accept the <a href="#" onClick={(e) => { e.preventDefault(); openModal('privacy'); }}>Privacy Policy</a>
        </label>
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
        <a href="#" onClick={(e) => { 
          e.preventDefault(); 
          setMode('login'); 
          setError(''); 
          setMessage(''); 
        }}>Already have an account? <strong>Login</strong></a>
      </div>
    </form>
  );

  const renderForgotPasswordForm = () => {
    switch (forgotPasswordStep) {
      case 'emailInput':
        return (
          <form onSubmit={handleForgotPasswordRequest} className="login-form">
            <p className="reset-info">Enter your email to receive a One-Time Password (OTP).</p>
            <div className="form-group">
              <label htmlFor="reset-email">Email Address</label>
              <input
                id="reset-email"
                type="email"
                placeholder="Enter your registered email"
                value={forgotPasswordEmail}
                onChange={(e) => { setForgotPasswordEmail(e.target.value); setError(''); setMessage(''); }}
                required
                disabled={loading}
              />
            </div>
            {error && <div className="error-message">{error}</div>}
            {message && <div className="success-message">{message}</div>}
            <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
              {loading ? 'Sending OTP...' : 'Send OTP'}
            </button>
            <div className="form-switcher">
              <a href="#" onClick={(e) => { 
                e.preventDefault(); 
                setMode('login'); 
                setError(''); 
                setMessage(''); 
                setForgotPasswordEmail(''); 
                setForgotPasswordStep('emailInput'); 
              }}>Back to <strong>Login</strong></a>
            </div>
          </form>
        );

      case 'otpInput':
        return (
          <form onSubmit={handleResetPassword} className="login-form">
            <p className="reset-info">Enter the OTP sent to {forgotPasswordEmail} and your new password.</p>
            <div className="form-group">
              <label htmlFor="otp">OTP</label>
              <input
                id="otp"
                type="text"
                placeholder="Enter the OTP from your email"
                value={otp}
                onChange={(e) => { setOtp(e.target.value); setError(''); setMessage(''); }}
                required
                disabled={loading}
              />
            </div>
            <div className="form-group">
              <label htmlFor="new-password">New Password</label>
              <PasswordInput
                id="new-password"
                name="newPassword"
                placeholder="Enter your new password"
                value={newPassword}
                onChange={(e) => { setNewPassword(e.target.value); setError(''); setMessage(''); }}
                required={true}
                disabled={loading}
                visibilityField="newPassword"
              />
            </div>
            <div className="form-group">
              <label htmlFor="confirm-new-password">Confirm New Password</label>
              <PasswordInput
                id="confirm-new-password"
                name="confirmNewPassword"
                placeholder="Confirm your new password"
                value={confirmNewPassword}
                onChange={(e) => { setConfirmNewPassword(e.target.value); setError(''); setMessage(''); }}
                required={true}
                disabled={loading}
                visibilityField="confirmPassword"
              />
            </div>
            {error && <div className="error-message">{error}</div>}
            {message && <div className="success-message">{message}</div>}
            <button type="submit" className={`login-btn ${loading ? 'loading' : ''}`} disabled={loading}>
              {loading ? 'Resetting Password...' : 'Reset Password'}
            </button>
            <div className="form-switcher">
              <a href="#" onClick={(e) => { 
                e.preventDefault(); 
                setForgotPasswordStep('emailInput'); 
                setError(''); 
                setMessage(''); 
              }}>Back to Email Input</a>
              <span> | </span>
              <a href="#" onClick={(e) => { 
                e.preventDefault(); 
                setMode('login'); 
                setError(''); 
                setMessage(''); 
                setForgotPasswordEmail(''); 
                setForgotPasswordStep('emailInput'); 
              }}>Back to <strong>Login</strong></a>
            </div>
          </form>
        );

      default:
        return null;
    }
  };

  return (
    <div className="login-container">
      {/* Theme Toggle Button */}
      <button className="theme-toggle" onClick={toggleTheme} aria-label="Toggle theme">
        <span className="icon">{darkMode ? '☀️' : '🌙'}</span>
      </button>

      <div className="login-background">
        <div className="login-card">
          <div className="login-header">
            <div className="logo">🛡️</div>
            <h1>
              {mode === 'login' ? 'NetGuard Login' : 
               mode === 'register' ? 'Create Account' : 'Reset Password'}
            </h1>
            <p>Smart Parental Internet Control</p>
          </div>

          {mode === 'login' && renderLoginForm()}
          {mode === 'register' && renderRegisterForm()}
          {mode === 'forgotPassword' && renderForgotPasswordForm()}

          <div className="login-footer">
            <p>Secure parental control for your home network</p>
          </div>
        </div>
      </div>

      {/* Terms and Privacy Modal */}
      <TermsModal 
        isOpen={modalState.isOpen} 
        onClose={closeModal} 
        type={modalState.type} 
      />
    </div>
  );
};

export default Login;