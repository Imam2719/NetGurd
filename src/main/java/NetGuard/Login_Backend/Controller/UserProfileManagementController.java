package NetGuard.Login_Backend.Controller;

import NetGuard.Login_Backend.dto.*;
import NetGuard.Login_Backend.Service.UserProfileManagementService;
import NetGuard.Login_Backend.exception.ValidationException;
import NetGuard.Login_Backend.util.JwtUtil;
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
@RequestMapping("/api/profile")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"},
        allowCredentials = "true",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@RequiredArgsConstructor
@Slf4j
public class UserProfileManagementController {

    private final UserProfileManagementService profileService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile(
            HttpServletRequest request) {

        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication token required"));
            }

            // FIXED: Validate token before extracting email
            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or expired token"));
            }

            String userEmail = jwtUtil.getEmailFromToken(token);
            if (userEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Unable to extract user information from token"));
            }

            log.info("Fetching profile for user: {}", userEmail);

            ApiResponse<UserProfileResponse> response = profileService.getCurrentUserProfile(userEmail);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch profile"));
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid profile data"));
        }

        try {
            String token = extractTokenFromRequest(httpRequest);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication token required"));
            }

            String userEmail = jwtUtil.getEmailFromToken(token);
            log.info("Updating profile for user: {}", userEmail);

            ApiResponse<UserProfileResponse> response = profileService.updateProfile(userEmail, request);

            if (response.isSuccess()) {
                log.info("Profile updated successfully for user: {}", userEmail);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (ValidationException e) {
            log.warn("Validation error during profile update: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update profile"));
        }
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileImageResponse>> uploadProfileImage(
            @RequestParam("image") MultipartFile image,
            HttpServletRequest request) {

        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication token required"));
            }

            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Image file is required"));
            }

            String userEmail = jwtUtil.getEmailFromToken(token);
            log.info("Uploading profile image for user: {}", userEmail);

            ApiResponse<ProfileImageResponse> response = profileService.uploadProfileImage(userEmail, image);

            if (response.isSuccess()) {
                log.info("Profile image uploaded successfully for user: {}", userEmail);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (ValidationException e) {
            log.warn("Validation error during image upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading profile image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload profile image"));
        }
    }

    @DeleteMapping("/image")
    public ResponseEntity<ApiResponse<Void>> deleteProfileImage(
            HttpServletRequest request) {

        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication token required"));
            }

            String userEmail = jwtUtil.getEmailFromToken(token);
            log.info("Deleting profile image for user: {}", userEmail);

            ApiResponse<Void> response = profileService.deleteProfileImage(userEmail);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting profile image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete profile image"));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid password data"));
        }

        try {
            String token = extractTokenFromRequest(httpRequest);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication token required"));
            }

            String userEmail = jwtUtil.getEmailFromToken(token);
            log.info("Changing password for user: {}", userEmail);

            ApiResponse<Void> response = profileService.changePassword(userEmail, request);

            if (response.isSuccess()) {
                log.info("Password changed successfully for user: {}", userEmail);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (ValidationException e) {
            log.warn("Validation error during password change: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to change password"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(
            HttpServletRequest request) {

        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication token required"));
            }

            String userEmail = jwtUtil.getEmailFromToken(token);
            log.info("Fetching user stats for: {}", userEmail);

            ApiResponse<UserStatsResponse> response = profileService.getUserStats(userEmail);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching user stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch user statistics"));
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            HttpServletRequest request) {

        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication token required"));
            }

            if (!jwtUtil.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or expired token"));
            }

            String userEmail = jwtUtil.getEmailFromToken(token);
            if (userEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Unable to extract user information from token"));
            }

            log.info("Deleting profile for user: {}", userEmail);

            ApiResponse<Void> response = profileService.deleteProfile(userEmail);

            if (response.isSuccess()) {
                log.info("Profile deleted successfully for user: {}", userEmail);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error deleting profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete profile"));
        }
    }

}