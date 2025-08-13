package NetGuard.Login_Backend.Service;

import NetGuard.Login_Backend.dto.*;
import NetGuard.Login_Backend.Model.Parent;
import NetGuard.Login_Backend.Model.PasswordResetToken;
import NetGuard.Login_Backend.Model.EmailVerificationToken;
import NetGuard.Login_Backend.Model.LoginAttempt;
import NetGuard.Login_Backend.Repository.ParentRepository;
import NetGuard.Login_Backend.Repository.PasswordResetTokenRepository;
import NetGuard.Login_Backend.Repository.EmailVerificationTokenRepository;
import NetGuard.Login_Backend.Repository.LoginAttemptRepository;
import NetGuard.Login_Backend.exception.AuthenticationException;
import NetGuard.Login_Backend.exception.ValidationException;
import NetGuard.Login_Backend.exception.AccountLockedException;
import NetGuard.Login_Backend.util.JwtUtil;
import NetGuard.Login_Backend.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final ParentRepository parentRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final ValidationUtil validationUtil;
    private final SecurityService securityService;

    @Value("${app.registration.require-email-verification:true}")
    private boolean requireEmailVerification;

    @Value("${app.oauth2.default-password-length:6}")
    private int defaultPasswordLength;

    private static final SecureRandom secureRandom = new SecureRandom();

    // Maximum file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Allowed image types
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    @Transactional
    public ApiResponse<LoginResponse> login(LoginRequest request, String clientIp, String userAgent) {
        try {
            // Check for account lockout
            if (securityService.isAccountLocked(request.getUsername(), clientIp)) {
                recordFailedLoginAttempt(request.getUsername(), clientIp, "Account locked");
                throw new AccountLockedException("Account is temporarily locked due to multiple failed attempts");
            }

            // Find parent by email
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(request.getUsername());

            if (parentOpt.isEmpty()) {
                recordFailedLoginAttempt(request.getUsername(), clientIp, "User not found");
                return ApiResponse.error("Invalid credentials");
            }

            Parent parent = parentOpt.get();

            // Check if email is verified (if required)
            if (requireEmailVerification && !parent.isEmailVerified()) {
                return ApiResponse.error("Please verify your email address before logging in");
            }

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), parent.getPassword())) {
                recordFailedLoginAttempt(request.getUsername(), clientIp, "Invalid password");
                return ApiResponse.error("Invalid credentials");
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(parent.getEmail(), parent.getName(), parent.getId());

            // Update last login
            parent.setLastLoginAt(LocalDateTime.now());
            parent.setLastLoginIp(clientIp);
            parentRepository.save(parent);

            // Record successful login
            recordSuccessfulLoginAttempt(request.getUsername(), clientIp);

            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .name(parent.getName())
                    .email(parent.getEmail())
                    .profileImageUrl(parent.getProfileImageUrl()) // FIXED: Use new method
                    .emailVerified(parent.isEmailVerified())
                    .build();

            return ApiResponse.success("Login successful", response);

        } catch (AccountLockedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Login error for email: {}", request.getUsername(), e);
            recordFailedLoginAttempt(request.getUsername(), clientIp, "System error");
            throw new AuthenticationException("Login failed");
        }
    }

    @Transactional
    public ApiResponse<RegisterResponse> register(RegisterRequest request, MultipartFile profileImage, String clientIp) {
        try {
            // Validate registration data
            validationUtil.validateRegistrationRequest(request);

            // Check if email already exists
            if (parentRepository.existsByEmail(request.getEmail())) {
                throw new ValidationException("Email address is already registered");
            }

            // Check if phone number already exists
            if (parentRepository.existsByPhone(request.getPhone())) {
                throw new ValidationException("Phone number is already registered");
            }

            // Create new parent entity
            Parent parent = buildParentFromRequest(request, clientIp);

            // Handle profile image upload - save directly to database as Base64
            if (profileImage != null && !profileImage.isEmpty()) {
                validateImageFile(profileImage);
                byte[] imageBytes = profileImage.getBytes();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                parent.setProfileImage(base64Image); // FIXED: Use new field
                parent.setProfileImageType(profileImage.getContentType()); // FIXED: Use new field
            }

            // Save parent
            Parent savedParent = parentRepository.save(parent);

            // Send email verification if required
            if (requireEmailVerification) {
                sendEmailVerification(savedParent);
            }

            RegisterResponse response = RegisterResponse.builder()
                    .id(savedParent.getId())
                    .email(savedParent.getEmail())
                    .name(savedParent.getName())
                    .emailVerificationRequired(requireEmailVerification)
                    .build();

            String message = requireEmailVerification
                    ? "Registration successful! Please check your email to verify your account."
                    : "Registration successful! You can now log in.";

            return ApiResponse.success(message, response);

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Registration error for email: {}", request.getEmail(), e);
            throw new RuntimeException("Registration failed due to internal error");
        }
    }

    @Transactional
    public ApiResponse<Void> initiateForgotPassword(ForgotPasswordRequest request, String clientIp) {
        try {
            // Always return success to prevent email enumeration
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(request.getEmail());

            if (parentOpt.isPresent()) {
                Parent parent = parentOpt.get();

                // Generate secure OTP
                String otp = generateSecureOtp();

                // Delete any existing unused tokens
                passwordResetTokenRepository.deleteByEmailAndUsedFalse(request.getEmail());

                // Create new reset token
                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .email(request.getEmail())
                        .otp(passwordEncoder.encode(otp)) // Hash the OTP
                        .expiresAt(LocalDateTime.now().plusMinutes(15))
                        .requestIp(clientIp)
                        .build();

                passwordResetTokenRepository.save(resetToken);

                // Send email with OTP (don't log the actual OTP)
                emailService.sendPasswordResetOtp(parent.getEmail(), parent.getName(), otp);

                log.info("Password reset OTP sent to email: {}", request.getEmail());
            } else {
                log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            }

            return ApiResponse.success("If your email is registered, you will receive a password reset code shortly.");

        } catch (Exception e) {
            log.error("Error initiating forgot password for email: {}", request.getEmail(), e);
            return ApiResponse.error("Unable to process password reset request at this time");
        }
    }

    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request, String clientIp) {
        try {
            // Find all valid reset tokens for email (we'll verify OTP against all)
            var validTokens = passwordResetTokenRepository
                    .findByEmailAndUsedFalseAndExpiresAtAfter(request.getEmail(), LocalDateTime.now());

            if (validTokens.isEmpty()) {
                return ApiResponse.error("Invalid or expired reset code");
            }

            // Check if any token matches the provided OTP
            Optional<PasswordResetToken> matchingToken = validTokens.stream()
                    .filter(token -> passwordEncoder.matches(request.getOtp(), token.getOtp()))
                    .findFirst();

            if (matchingToken.isEmpty()) {
                return ApiResponse.error("Invalid reset code");
            }

            // Find parent
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(request.getEmail());
            if (parentOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            Parent parent = parentOpt.get();

            // Validate new password
            validationUtil.validatePassword(request.getNewPassword());

            // Update password
            parent.setPassword(passwordEncoder.encode(request.getNewPassword()));
            parent.setPasswordUpdatedAt(LocalDateTime.now());
            parentRepository.save(parent);

            // Mark all tokens as used for this email
            validTokens.forEach(token -> {
                token.setUsed(true);
                token.setUsedAt(LocalDateTime.now());
                token.setUsedFromIp(clientIp);
            });
            passwordResetTokenRepository.saveAll(validTokens);

            // Send confirmation email
            emailService.sendPasswordResetConfirmation(parent.getEmail(), parent.getName());

            return ApiResponse.success("Password has been reset successfully. You can now log in with your new password.");

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error resetting password for email: {}", request.getEmail(), e);
            return ApiResponse.error("Unable to reset password at this time");
        }
    }

    @Transactional
    public ApiResponse<Void> verifyEmail(EmailVerificationRequest request) {
        try {
            Optional<EmailVerificationToken> tokenOpt = emailVerificationTokenRepository
                    .findByTokenAndUsedFalseAndExpiresAtAfter(request.getToken(), LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                return ApiResponse.error("Invalid or expired verification token");
            }

            EmailVerificationToken token = tokenOpt.get();
            Optional<Parent> parentOpt = parentRepository.findByEmail(token.getEmail());

            if (parentOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            Parent parent = parentOpt.get();
            parent.setEmailVerified(true);
            parent.setEmailVerifiedAt(LocalDateTime.now());
            parentRepository.save(parent);

            token.setUsed(true);
            token.setUsedAt(LocalDateTime.now());
            emailVerificationTokenRepository.save(token);

            return ApiResponse.success("Email verified successfully! You can now log in.");

        } catch (Exception e) {
            log.error("Error verifying email for token: {}", request.getToken(), e);
            return ApiResponse.error("Email verification failed");
        }
    }

    @Transactional
    public ApiResponse<Void> resendEmailVerification(ResendVerificationRequest request, String clientIp) {
        try {
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(request.getEmail());

            if (parentOpt.isEmpty()) {
                // Don't reveal if email exists
                return ApiResponse.success("If your email is registered, a verification link will be sent.");
            }

            Parent parent = parentOpt.get();

            if (parent.isEmailVerified()) {
                return ApiResponse.error("Email is already verified");
            }

            sendEmailVerification(parent);

            return ApiResponse.success("Verification email has been resent.");

        } catch (Exception e) {
            log.error("Error resending email verification for: {}", request.getEmail(), e);
            return ApiResponse.error("Unable to resend verification email");
        }
    }

    // OAuth2 Methods
    @Transactional
    public ApiResponse<OAuth2SignupResponse> completeOAuth2Signup(OAuth2SignupRequest request, String clientIp) {
        try {
            // Validate OAuth2 token
            if (!jwtUtil.isOAuth2TokenValid(request.getOauth2Token())) {
                return ApiResponse.error("Invalid or expired OAuth2 token");
            }

            // Extract user info from token
            String email = jwtUtil.getEmailFromToken(request.getOauth2Token());
            String name = jwtUtil.getNameFromToken(request.getOauth2Token());
            String profilePictureUrl = jwtUtil.getProfilePictureUrlFromToken(request.getOauth2Token());

            log.info("Processing OAuth2 signup for email: {}, name: {}, profilePictureUrl: {}",
                    email, name, profilePictureUrl);

            // Check if user already exists
            if (parentRepository.existsByEmail(email)) {
                log.warn("User already exists with email: {}", email);
                return ApiResponse.error("User already exists with this email");
            }

            // Validate phone number
            if (parentRepository.existsByPhone(request.getPhone())) {
                log.warn("Phone number already registered: {}", request.getPhone());
                return ApiResponse.error("Phone number is already registered");
            }

            // Validate input data
            validationUtil.validatePhone(request.getPhone());
            validationUtil.validateAge(request.getAge());

            // Generate random password
            String temporaryPassword = generateRandomPassword(defaultPasswordLength);
            log.info("Generated temporary password for OAuth2 user: {}", email);

            // Create new parent with all required fields including profile picture
            Parent parent = Parent.builder()
                    .name(name != null ? name.trim() : email.split("@")[0])
                    .email(email.toLowerCase().trim())
                    .phone(request.getPhone().trim())
                    .age(request.getAge())
                    .password(passwordEncoder.encode(temporaryPassword))
                    .registrationIp(clientIp)
                    .active(true)
                    .emailVerified(true)
                    .emailVerifiedAt(LocalDateTime.now())
                    .termsAcceptedAt(request.getAcceptTerms() ? LocalDateTime.now() : null)
                    .privacyAcceptedAt(request.getAcceptPrivacy() ? LocalDateTime.now() : null)
                    .build();

            // Handle Google profile image - store URL directly
            if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                try {
                    // For Google profile images, we store the URL directly with special type
                    parent.setProfileImage(profilePictureUrl); // FIXED: Use new field
                    parent.setProfileImageType("image/url"); // FIXED: Special type for URLs
                    log.info("Set profile image URL for OAuth2 user: {}", profilePictureUrl);
                } catch (Exception imageError) {
                    log.warn("Failed to set profile image for OAuth2 user: {}, error: {}", email, imageError.getMessage());
                }
            }

            // Save to database
            Parent savedParent = parentRepository.save(parent);
            log.info("Successfully saved OAuth2 parent to database with ID: {} and profile image: {}",
                    savedParent.getId(), savedParent.hasProfileImage() ? "Yes" : "No");

            // Verify the save operation
            if (savedParent.getId() == null) {
                log.error("Failed to save parent - ID is null");
                throw new RuntimeException("Failed to save user to database");
            }

            // Generate JWT token
            String jwtToken = jwtUtil.generateToken(savedParent.getEmail(), savedParent.getName(), savedParent.getId());

            // Send welcome email
            try {
                emailService.sendOAuth2WelcomeEmail(savedParent.getEmail(), savedParent.getName(), temporaryPassword);
                log.info("OAuth2 welcome email sent to: {}", savedParent.getEmail());
            } catch (Exception emailError) {
                log.warn("Failed to send OAuth2 welcome email to: {}, error: {}", savedParent.getEmail(), emailError.getMessage());
            }

            OAuth2SignupResponse response = OAuth2SignupResponse.builder()
                    .token(jwtToken)
                    .name(savedParent.getName())
                    .email(savedParent.getEmail())
                    .profileImageUrl(savedParent.getProfileImageUrl()) // FIXED: Use new method
                    .temporaryPassword(temporaryPassword)
                    .message("Registration successful! Check your email for login credentials.")
                    .build();

            log.info("OAuth2 signup completed successfully for: {} with profile image: {}",
                    savedParent.getEmail(), savedParent.hasProfileImage() ? "Yes" : "No");
            return ApiResponse.success("OAuth2 registration successful", response);

        } catch (ValidationException e) {
            log.error("Validation error during OAuth2 signup: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error completing OAuth2 signup", e);
            throw new RuntimeException("OAuth2 signup failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<LoginResponse> oauth2Login(OAuth2LoginRequest request, String clientIp) {
        try {
            // Validate OAuth2 token
            if (!jwtUtil.isOAuth2TokenValid(request.getOauth2Token())) {
                return ApiResponse.error("Invalid or expired OAuth2 token");
            }

            // Extract user info from token
            String email = jwtUtil.getEmailFromToken(request.getOauth2Token());
            String profilePictureUrl = jwtUtil.getProfilePictureUrlFromToken(request.getOauth2Token());

            // Find existing user
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(email);
            if (parentOpt.isEmpty()) {
                return ApiResponse.error("User not found. Please complete registration first.");
            }

            Parent parent = parentOpt.get();

            // Update profile picture if it's empty and we have one from Google
            if (!parent.hasProfileImage() && profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                parent.setProfileImage(profilePictureUrl); // FIXED: Use new field
                parent.setProfileImageType("image/url"); // FIXED: Special type for URLs
                parentRepository.save(parent);
                log.info("Updated profile picture for existing user: {}", email);
            }

            // Generate JWT token
            String jwtToken = jwtUtil.generateToken(parent.getEmail(), parent.getName(), parent.getId());

            // Update last login
            parent.setLastLoginAt(LocalDateTime.now());
            parent.setLastLoginIp(clientIp);
            parentRepository.save(parent);

            // Record successful login
            recordSuccessfulLoginAttempt(email, clientIp);

            LoginResponse response = LoginResponse.builder()
                    .token(jwtToken)
                    .name(parent.getName())
                    .email(parent.getEmail())
                    .profileImageUrl(parent.getProfileImageUrl()) // FIXED: Use new method
                    .emailVerified(parent.isEmailVerified())
                    .build();

            log.info("OAuth2 login successful for: {} with profile image: {}", email, parent.hasProfileImage() ? "Yes" : "No");
            return ApiResponse.success("OAuth2 login successful", response);

        } catch (Exception e) {
            log.error("Error during OAuth2 login", e);
            return ApiResponse.error("OAuth2 login failed");
        }
    }

    // Helper Methods
    private Parent buildParentFromRequest(RegisterRequest request, String clientIp) {
        return Parent.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone().trim())
                .age(request.getAge())
                .password(passwordEncoder.encode(request.getPassword()))
                .registrationIp(clientIp)
                .active(true)
                .emailVerified(!requireEmailVerification)
                .termsAcceptedAt(request.getAcceptTerms() != null && request.getAcceptTerms() ? LocalDateTime.now() : null)
                .privacyAcceptedAt(request.getAcceptPrivacy() != null && request.getAcceptPrivacy() ? LocalDateTime.now() : null)
                .build();
    }

    private void sendEmailVerification(Parent parent) {
        try {
            // Delete any existing tokens
            emailVerificationTokenRepository.deleteByEmailAndUsedFalse(parent.getEmail());

            // Create new verification token
            String token = UUID.randomUUID().toString();
            EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                    .email(parent.getEmail())
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            emailVerificationTokenRepository.save(verificationToken);
            emailService.sendEmailVerification(parent.getEmail(), parent.getName(), token);

        } catch (Exception e) {
            log.error("Failed to send email verification to: {}", parent.getEmail(), e);
        }
    }

    private void validateImageFile(MultipartFile image) throws ValidationException {
        // Check file size
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("Image file size must be less than 5MB");
        }

        // Check file type
        String contentType = image.getContentType();
        if (contentType == null) {
            throw new ValidationException("Invalid image file");
        }

        boolean isValidType = false;
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(contentType.toLowerCase())) {
                isValidType = true;
                break;
            }
        }

        if (!isValidType) {
            throw new ValidationException("Invalid image type. Allowed types: JPEG, PNG, GIF, WebP");
        }

        // Check if file is actually an image (basic validation)
        if (image.getOriginalFilename() == null || image.isEmpty()) {
            throw new ValidationException("Invalid image file");
        }
    }

    private String generateSecureOtp() {
        return String.format("%06d", secureRandom.nextInt(999999));
    }

    private String generateRandomPassword(int length) {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            password.append(secureRandom.nextInt(10));
        }
        return password.toString();
    }

    private void recordSuccessfulLoginAttempt(String email, String clientIp) {
        try {
            LoginAttempt attempt = LoginAttempt.builder()
                    .email(email)
                    .ipAddress(clientIp)
                    .successful(true)
                    .attemptTime(LocalDateTime.now())
                    .build();
            loginAttemptRepository.save(attempt);
        } catch (Exception e) {
            log.warn("Failed to record successful login attempt", e);
        }
    }

    private void recordFailedLoginAttempt(String email, String clientIp, String reason) {
        try {
            LoginAttempt attempt = LoginAttempt.builder()
                    .email(email)
                    .ipAddress(clientIp)
                    .successful(false)
                    .failureReason(reason)
                    .attemptTime(LocalDateTime.now())
                    .build();
            loginAttemptRepository.save(attempt);
        } catch (Exception e) {
            log.warn("Failed to record failed login attempt", e);
        }
    }

    @Transactional
    public void cleanupExpiredTokens() {
        try {
            passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            emailVerificationTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            log.info("Cleaned up expired tokens");
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens", e);
        }
    }

    @Transactional
    public void cleanupOldLoginAttempts() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            loginAttemptRepository.deleteByAttemptTimeBefore(cutoff);
            log.info("Cleaned up old login attempts");
        } catch (Exception e) {
            log.error("Error cleaning up old login attempts", e);
        }
    }
}