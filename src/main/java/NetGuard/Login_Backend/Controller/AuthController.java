package NetGuard.Login_Backend.Controller;

import NetGuard.Login_Backend.dto.*;
import NetGuard.Login_Backend.Service.AuthService;
import NetGuard.Login_Backend.exception.AuthenticationException;
import NetGuard.Login_Backend.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid input data"));
        }

        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Login attempt for email: {} from IP: {}", request.getUsername(), clientIp);

        try {
            ApiResponse<LoginResponse> response = authService.login(request, clientIp, userAgent);

            if (response.isSuccess()) {
                log.info("Successful login for email: {}", request.getUsername());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed login attempt for email: {} - {}", request.getUsername(), response.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (AuthenticationException e) {
            log.error("Authentication error for email: {} - {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication failed"));
        } catch (Exception e) {
            log.error("Unexpected error during login for email: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @RequestPart("request") @Valid RegisterRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid registration data"));
        }

        String clientIp = getClientIpAddress(httpRequest);
        log.info("Registration attempt for email: {} from IP: {}", request.getEmail(), clientIp);

        try {
            ApiResponse<RegisterResponse> response = authService.register(request, profileImage, clientIp);

            if (response.isSuccess()) {
                log.info("Successful registration for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (ValidationException e) {
            log.warn("Validation error during registration for email: {} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during registration for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration failed due to internal error"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid email format"));
        }

        String clientIp = getClientIpAddress(httpRequest);
        log.info("Forgot password request for email: {} from IP: {}", request.getEmail(), clientIp);

        try {
            ApiResponse<Void> response = authService.initiateForgotPassword(request, clientIp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing forgot password request for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to process request at this time"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid reset password data"));
        }

        String clientIp = getClientIpAddress(httpRequest);
        log.info("Reset password request for email: {} from IP: {}", request.getEmail(), clientIp);

        try {
            ApiResponse<Void> response = authService.resetPassword(request, clientIp);

            if (response.isSuccess()) {
                log.info("Successful password reset for email: {}", request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (ValidationException e) {
            log.warn("Validation error during password reset for email: {} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing password reset for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to process password reset"));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request) {

        log.info("Email verification request for token: {}", request.getToken());

        try {
            ApiResponse<Void> response = authService.verifyEmail(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing email verification for token: {}", request.getToken(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired verification token"));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        log.info("Resend verification request for email: {} from IP: {}", request.getEmail(), clientIp);

        try {
            ApiResponse<Void> response = authService.resendEmailVerification(request, clientIp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resending verification for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to resend verification email"));
        }
    }

    // OAuth2 endpoints
    @PostMapping("/oauth2/signup")
    public ResponseEntity<ApiResponse<OAuth2SignupResponse>> completeOAuth2Signup(
            @Valid @RequestBody OAuth2SignupRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid OAuth2 signup data"));
        }

        String clientIp = getClientIpAddress(httpRequest);
        log.info("OAuth2 signup completion from IP: {}", clientIp);

        try {
            ApiResponse<OAuth2SignupResponse> response = authService.completeOAuth2Signup(request, clientIp);

            if (response.isSuccess()) {
                log.info("Successful OAuth2 signup completion");
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (ValidationException e) {
            log.warn("Validation error during OAuth2 signup: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during OAuth2 signup completion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("OAuth2 signup failed due to internal error"));
        }
    }

    @PostMapping("/oauth2/login")
    public ResponseEntity<ApiResponse<LoginResponse>> oauth2Login(
            @Valid @RequestBody OAuth2LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        log.info("OAuth2 login attempt from IP: {}", clientIp);

        try {
            ApiResponse<LoginResponse> response = authService.oauth2Login(request, clientIp);

            if (response.isSuccess()) {
                log.info("Successful OAuth2 login");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            log.error("Error during OAuth2 login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("OAuth2 login failed"));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

}