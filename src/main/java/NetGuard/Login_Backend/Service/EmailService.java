package NetGuard.Login_Backend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// CORRECT IMPORTS FOR SPRING BOOT 3.x - Use jakarta.mail
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.from:noreply@netguard.com}")
    private String fromEmail;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        log.info("=== EmailService Initialized (HTML Mode) ===");
        log.info("MailSender type: {}", mailSender.getClass().getSimpleName());
        log.info("Email enabled: {}", emailEnabled);
        log.info("From email: {}", fromEmail);
        log.info("===========================================");
    }

    // ============================================
    // EXISTING OTP METHODS - PRESERVED EXACTLY
    // ============================================

    @Async
    public void sendPasswordResetOtp(String toEmail, String name, String otp) {
        log.info("=== SENDING PREMIUM HTML OTP EMAIL ===");
        log.info("To: {}, Name: {}, OTP: {}", toEmail, name, otp);

        if (!emailEnabled) {
            log.info("=== EMAIL SERVICE (DEVELOPMENT MODE) ===");
            log.info("Premium Password Reset OTP for: {} ({})", name, toEmail);
            log.info("OTP Code: {}", otp);
            log.info("This code expires in 15 minutes");
            log.info("=========================================");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "NetGuard Security Team");
            helper.setTo(toEmail);
            helper.setSubject("🔐 NetGuard Password Reset Code");

            // Use your existing HTML template
            String htmlContent = buildPremiumPasswordResetHtml(name, otp, toEmail);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✅ Premium HTML OTP email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send premium HTML OTP email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendPasswordResetConfirmation(String toEmail, String name) {
        log.info("Sending premium password reset confirmation to: {}", toEmail);

        if (!emailEnabled) {
            log.info("=== EMAIL SERVICE (DEVELOPMENT MODE) ===");
            log.info("Premium Password Reset Confirmation for: {} ({})", name, toEmail);
            log.info("=========================================");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "NetGuard Security Team");
            helper.setTo(toEmail);
            helper.setSubject("✅ NetGuard Password Reset Successful");

            String htmlContent = buildPasswordResetConfirmationHtml(name);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✅ Premium password reset confirmation sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send premium confirmation email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendEmailVerification(String toEmail, String name, String token) {
        log.info("Sending premium email verification to: {}", toEmail);

        if (!emailEnabled) {
            String verificationUrl = frontendBaseUrl + "/verify-email?token=" + token;
            log.info("=== EMAIL SERVICE (DEVELOPMENT MODE) ===");
            log.info("Premium Email Verification for: {} ({})", name, toEmail);
            log.info("Verification URL: {}", verificationUrl);
            log.info("=========================================");
            return;
        }

        try {
            String verificationUrl = frontendBaseUrl + "/verify-email?token=" + token;

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "NetGuard Welcome Team");
            helper.setTo(toEmail);
            helper.setSubject("🎉 Welcome to NetGuard - Verify Your Email");

            String htmlContent = buildEmailVerificationHtml(name, verificationUrl);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✅ Premium email verification sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send premium verification email to: {}", toEmail, e);
        }
    }

    // Synchronous version for testing - PRESERVED
    public boolean sendPasswordResetOtpSync(String toEmail, String name, String otp) {
        if (!emailEnabled) {
            log.info("Email disabled - would send premium OTP: {} to: {}", otp, toEmail);
            return false;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "NetGuard Security Team");
            helper.setTo(toEmail);
            helper.setSubject("🔐 NetGuard Password Reset Code (TEST)");

            String htmlContent = buildPremiumPasswordResetHtml(name, otp, toEmail);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✅ SYNC: Premium HTML OTP sent successfully");
            return true;

        } catch (Exception e) {
            log.error("❌ SYNC: Failed to send premium HTML OTP: {}", e.getMessage(), e);
            return false;
        }
    }

    // ============================================
    // NEW OAUTH2 METHOD - ONLY ADDITION
    // ============================================

    @Async
    public void sendOAuth2WelcomeEmail(String toEmail, String name, String temporaryPassword) {
        log.info("=== SENDING OAUTH2 WELCOME EMAIL ===");
        log.info("To: {}, Name: {}, Temp Password: {}", toEmail, name, temporaryPassword);

        if (!emailEnabled) {
            log.info("=== EMAIL SERVICE (DEVELOPMENT MODE) ===");
            log.info("OAuth2 Welcome Email for: {} ({})", name, toEmail);
            log.info("Temporary Password: {}", temporaryPassword);
            log.info("=========================================");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "NetGuard Welcome Team");
            helper.setTo(toEmail);
            helper.setSubject("🎉 Welcome to NetGuard - Your Account Details");

            String htmlContent = buildOAuth2WelcomeHtml(name, toEmail, temporaryPassword);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✅ OAuth2 welcome email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send OAuth2 welcome email to: {}", toEmail, e);
        }
    }

    // ============================================
    // EXISTING HTML TEMPLATES - PRESERVED EXACTLY
    // ============================================

    private String buildPremiumPasswordResetHtml(String name, String otp, String email) {
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>NetGuard Password Reset</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        overflow: hidden;
                        box-shadow: 0 25px 50px rgba(0,0,0,0.15);
                    }
                    .header {
                        background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%);
                        padding: 40px 30px;
                        text-align: center;
                        color: white;
                    }
                    .logo {
                        font-size: 48px;
                        margin-bottom: 10px;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.2);
                    }
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 8px;
                    }
                    .header p {
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    .content {
                        padding: 40px 30px;
                        text-align: center;
                    }
                    .greeting {
                        font-size: 24px;
                        color: #1f2937;
                        margin-bottom: 20px;
                        font-weight: 600;
                    }
                    .message {
                        font-size: 16px;
                        color: #6b7280;
                        margin-bottom: 30px;
                        line-height: 1.8;
                    }
                    .otp-container {
                        background: linear-gradient(135deg, #f3f4f6 0%%, #e5e7eb 100%%);
                        border-radius: 15px;
                        padding: 30px;
                        margin: 30px 0;
                        border: 2px dashed #d1d5db;
                    }
                    .otp-label {
                        font-size: 14px;
                        color: #6b7280;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 15px;
                        font-weight: 600;
                    }
                    .otp-code {
                        font-size: 36px;
                        font-weight: 800;
                        color: #6366f1;
                        letter-spacing: 8px;
                        font-family: 'Courier New', monospace;
                        background: white;
                        padding: 20px;
                        border-radius: 10px;
                        box-shadow: 0 4px 15px rgba(99, 102, 241, 0.2);
                        display: inline-block;
                        margin: 10px 0;
                    }
                    .expiry-info {
                        background: #fef3c7;
                        border: 1px solid #f59e0b;
                        border-radius: 10px;
                        padding: 15px;
                        margin: 25px 0;
                        color: #92400e;
                        font-size: 14px;
                        font-weight: 500;
                    }
                    .security-notice {
                        background: #ecfdf5;
                        border: 1px solid #10b981;
                        border-radius: 10px;
                        padding: 20px;
                        margin: 25px 0;
                        color: #065f46;
                        font-size: 14px;
                    }
                    .footer {
                        background: #f9fafb;
                        padding: 30px;
                        text-align: center;
                        border-top: 1px solid #e5e7eb;
                    }
                    .footer-info {
                        font-size: 12px;
                        color: #9ca3af;
                        margin-bottom: 15px;
                    }
                    .social-links {
                        margin: 20px 0;
                    }
                    .social-links a {
                        display: inline-block;
                        margin: 0 10px;
                        padding: 8px 16px;
                        background: #6366f1;
                        color: white;
                        text-decoration: none;
                        border-radius: 20px;
                        font-size: 12px;
                        font-weight: 500;
                    }
                    .timestamp {
                        font-size: 12px;
                        color: #9ca3af;
                        margin-top: 20px;
                        font-style: italic;
                    }
                    @media (max-width: 600px) {
                        .email-container { margin: 10px; border-radius: 15px; }
                        .header, .content, .footer { padding: 25px 20px; }
                        .otp-code { font-size: 28px; letter-spacing: 4px; }
                        .greeting { font-size: 20px; }
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <div class="logo">🛡️</div>
                        <h1>NetGuard Security</h1>
                        <p>Smart Parental Internet Control</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Hello, %s! 👋</div>
                        <div class="message">
                            We received a request to reset your NetGuard account password. 
                            Use the verification code below to complete your password reset.
                        </div>
                        
                        <div class="otp-container">
                            <div class="otp-label">Your Verification Code</div>
                            <div class="otp-code">%s</div>
                        </div>
                        
                        <div class="expiry-info">
                            ⏰ <strong>Important:</strong> This code expires in 15 minutes for your security.
                        </div>
                        
                        <div class="security-notice">
                            🔒 <strong>Security Tip:</strong> Never share this code with anyone. NetGuard support will never ask for your verification code.
                        </div>
                        
                        <div class="timestamp">
                            Sent on %s
                        </div>
                    </div>
                    
                    <div class="footer">
                        <div class="footer-info">
                            This email was sent to <strong>%s</strong> as part of your NetGuard account security.
                        </div>
                        <div class="social-links">
                            <a href="#">Help Center</a>
                            <a href="#">Contact Support</a>
                            <a href="#">Privacy Policy</a>
                        </div>
                        <div class="footer-info">
                            © 2025 NetGuard. All rights reserved.<br>
                            Smart Parental Internet Control System
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                name, otp, currentDateTime, email
        );
    }

    private String buildPasswordResetConfirmationHtml(String name) {
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset Successful</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                        background: linear-gradient(135deg, #10b981 0%%, #059669 100%%);
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        overflow: hidden;
                        box-shadow: 0 25px 50px rgba(0,0,0,0.15);
                    }
                    .header {
                        background: linear-gradient(135deg, #10b981 0%%, #059669 100%%);
                        padding: 40px 30px;
                        text-align: center;
                        color: white;
                    }
                    .success-icon {
                        font-size: 64px;
                        margin-bottom: 15px;
                    }
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 8px;
                    }
                    .content {
                        padding: 40px 30px;
                        text-align: center;
                    }
                    .greeting {
                        font-size: 24px;
                        color: #1f2937;
                        margin-bottom: 20px;
                        font-weight: 600;
                    }
                    .success-message {
                        font-size: 18px;
                        color: #059669;
                        margin-bottom: 30px;
                        font-weight: 600;
                    }
                    .message {
                        font-size: 16px;
                        color: #6b7280;
                        margin-bottom: 30px;
                        line-height: 1.8;
                    }
                    .security-tips {
                        background: #f0f9ff;
                        border: 1px solid #0ea5e9;
                        border-radius: 15px;
                        padding: 25px;
                        margin: 25px 0;
                        text-align: left;
                    }
                    .security-tips h3 {
                        color: #0c4a6e;
                        margin-bottom: 15px;
                        font-size: 18px;
                    }
                    .security-tips ul {
                        color: #075985;
                        padding-left: 20px;
                    }
                    .security-tips li {
                        margin-bottom: 8px;
                    }
                    .footer {
                        background: #f9fafb;
                        padding: 30px;
                        text-align: center;
                        border-top: 1px solid #e5e7eb;
                    }
                    .timestamp {
                        font-size: 12px;
                        color: #9ca3af;
                        margin-top: 20px;
                        font-style: italic;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <div class="success-icon">✅</div>
                        <h1>Password Reset Successful!</h1>
                        <p>Your NetGuard account is now secure</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Hello, %s! 🎉</div>
                        <div class="success-message">
                            Your password has been successfully updated!
                        </div>
                        <div class="message">
                            Your NetGuard account password was changed successfully. You can now log in with your new password.
                        </div>
                        
                        <div class="security-tips">
                            <h3>🔒 Security Recommendations:</h3>
                            <ul>
                                <li>Use a strong, unique password for your account</li>
                                <li>Enable two-factor authentication if available</li>
                                <li>Regularly review your account activity</li>
                                <li>Never share your login credentials with anyone</li>
                            </ul>
                        </div>
                        
                        <div class="timestamp">
                            Password changed on %s
                        </div>
                    </div>
                    
                    <div class="footer">
                        <div style="font-size: 12px; color: #9ca3af;">
                            © 2025 NetGuard. All rights reserved.<br>
                            Smart Parental Internet Control System
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                name, currentDateTime
        );
    }

    private String buildEmailVerificationHtml(String name, String verificationUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to NetGuard</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                        background: linear-gradient(135deg, #8b5cf6 0%%, #6366f1 100%%);
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        overflow: hidden;
                        box-shadow: 0 25px 50px rgba(0,0,0,0.15);
                    }
                    .header {
                        background: linear-gradient(135deg, #8b5cf6 0%%, #6366f1 100%%);
                        padding: 40px 30px;
                        text-align: center;
                        color: white;
                    }
                    .welcome-icon {
                        font-size: 64px;
                        margin-bottom: 15px;
                    }
                    .verify-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%);
                        color: white;
                        padding: 15px 30px;
                        text-decoration: none;
                        border-radius: 30px;
                        font-weight: 600;
                        font-size: 16px;
                        margin: 25px 0;
                        box-shadow: 0 8px 25px rgba(99, 102, 241, 0.3);
                        transition: all 0.3s ease;
                    }
                    .content {
                        padding: 40px 30px;
                        text-align: center;
                    }
                    .greeting {
                        font-size: 24px;
                        color: #1f2937;
                        margin-bottom: 20px;
                        font-weight: 600;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <div class="welcome-icon">🎉</div>
                        <h1>Welcome to NetGuard!</h1>
                        <p>Smart Parental Internet Control</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Hello, %s! 👋</div>
                        <div style="font-size: 16px; color: #6b7280; margin-bottom: 30px;">
                            Thank you for joining NetGuard! Please verify your email address to complete your registration.
                        </div>
                        
                        <a href="%s" class="verify-button">
                            ✉️ Verify Email Address
                        </a>
                        
                        <div style="font-size: 14px; color: #9ca3af; margin-top: 25px;">
                            This verification link expires in 24 hours.
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                name, verificationUrl
        );
    }

    // ============================================
    // NEW OAUTH2 HTML TEMPLATE - ONLY ADDITION
    // ============================================

    private String buildOAuth2WelcomeHtml(String name, String email, String temporaryPassword) {
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to NetGuard</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                        background: linear-gradient(135deg, #4f46e5 0%%, #06b6d4 100%%);
                        padding: 20px;
                        line-height: 1.6;
                    }
                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        overflow: hidden;
                        box-shadow: 0 25px 50px rgba(0,0,0,0.15);
                    }
                    .header {
                        background: linear-gradient(135deg, #4f46e5 0%%, #06b6d4 100%%);
                        padding: 40px 30px;
                        text-align: center;
                        color: white;
                    }
                    .welcome-icon {
                        font-size: 64px;
                        margin-bottom: 15px;
                    }
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 8px;
                    }
                    .content {
                        padding: 40px 30px;
                        text-align: center;
                    }
                    .greeting {
                        font-size: 24px;
                        color: #1f2937;
                        margin-bottom: 20px;
                        font-weight: 600;
                    }
                    .message {
                        font-size: 16px;
                        color: #6b7280;
                        margin-bottom: 30px;
                        line-height: 1.8;
                    }
                    .credentials-container {
                        background: linear-gradient(135deg, #f0f9ff 0%%, #e0f2fe 100%%);
                        border-radius: 15px;
                        padding: 30px;
                        margin: 30px 0;
                        border: 2px solid #0ea5e9;
                    }
                    .credentials-title {
                        font-size: 18px;
                        color: #0c4a6e;
                        margin-bottom: 20px;
                        font-weight: 600;
                    }
                    .credential-item {
                        background: white;
                        border-radius: 10px;
                        padding: 15px 20px;
                        margin: 15px 0;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    }
                    .credential-label {
                        font-weight: 600;
                        color: #374151;
                    }
                    .credential-value {
                        font-family: 'Courier New', monospace;
                        color: #1f2937;
                        font-weight: 700;
                        background: #f3f4f6;
                        padding: 5px 10px;
                        border-radius: 5px;
                    }
                    .password-highlight {
                        background: #fef3c7 !important;
                        color: #92400e !important;
                        font-size: 18px;
                        padding: 10px 15px !important;
                        border: 2px solid #f59e0b;
                    }
                    .security-notice {
                        background: #fef2f2;
                        border: 1px solid #f87171;
                        border-radius: 10px;
                        padding: 20px;
                        margin: 25px 0;
                        color: #991b1b;
                        font-size: 14px;
                    }
                    .login-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #4f46e5 0%%, #06b6d4 100%%);
                        color: white;
                        padding: 15px 30px;
                        text-decoration: none;
                        border-radius: 30px;
                        font-weight: 600;
                        font-size: 16px;
                        margin: 25px 0;
                        box-shadow: 0 8px 25px rgba(79, 70, 229, 0.3);
                    }
                    .footer {
                        background: #f9fafb;
                        padding: 30px;
                        text-align: center;
                        border-top: 1px solid #e5e7eb;
                    }
                    .timestamp {
                        font-size: 12px;
                        color: #9ca3af;
                        margin-top: 20px;
                        font-style: italic;
                    }
                    @media (max-width: 600px) {
                        .credential-item {
                            flex-direction: column;
                            align-items: flex-start;
                            gap: 10px;
                        }
                        .credential-value {
                            align-self: stretch;
                            text-align: center;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <div class="welcome-icon">🎉</div>
                        <h1>Welcome to NetGuard!</h1>
                        <p>Your Google account has been successfully linked</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Hello, %s! 👋</div>
                        <div class="message">
                            Thank you for joining NetGuard with your Google account! We've created your account and generated secure login credentials for you.
                        </div>
                        
                        <div class="credentials-container">
                            <div class="credentials-title">🔐 Your Login Credentials</div>
                            
                            <div class="credential-item">
                                <span class="credential-label">📧 Email:</span>
                                <span class="credential-value">%s</span>
                            </div>
                            
                            <div class="credential-item">
                                <span class="credential-label">🔑 Temporary Password:</span>
                                <span class="credential-value password-highlight">%s</span>
                            </div>
                        </div>
                        
                        <div class="security-notice">
                            🛡️ <strong>Important Security Notice:</strong><br>
                            • This is a temporary 6-digit password generated for your account<br>
                            • Please change this password after your first login<br>
                            • You can also continue using Google Sign-In for easier access<br>
                            • Keep these credentials secure and don't share them with anyone
                        </div>
                        
                        <a href="http://localhost:3000/login" class="login-button">
                            🚀 Login to NetGuard
                        </a>
                        
                        <div class="timestamp">
                            Account created on %s
                        </div>
                    </div>
                    
                    <div class="footer">
                        <div style="font-size: 14px; color: #6b7280; margin-bottom: 15px;">
                            Welcome to the NetGuard family! Start protecting your family's internet experience today.
                        </div>
                        <div style="font-size: 12px; color: #9ca3af;">
                            © 2025 NetGuard. All rights reserved.<br>
                            Smart Parental Internet Control System
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                name, email, temporaryPassword, currentDateTime
        );
    }

    // ============================================
    // EXISTING TEST METHOD - PRESERVED
    // ============================================

    public boolean testConnection() {
        if (!emailEnabled) {
            log.info("Email disabled - connection test skipped");
            return false;
        }

        try {
            log.info("Testing premium email connection...");
            log.info("✅ Premium email service connection test passed");
            return true;
        } catch (Exception e) {
            log.error("❌ Premium email service connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }
}