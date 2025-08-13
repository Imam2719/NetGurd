package NetGuard.Login_Backend.Service;

import NetGuard.Login_Backend.dto.*;
import NetGuard.Login_Backend.Model.Parent;
import NetGuard.Login_Backend.Repository.ParentRepository;
import NetGuard.Login_Backend.exception.ValidationException;
import NetGuard.Login_Backend.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileManagementService {

    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidationUtil validationUtil;

    // Maximum file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Allowed image types
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    public ApiResponse<UserProfileResponse> getCurrentUserProfile(String email) {
        try {
            log.info("Looking for user with email: {}", email);

            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(email);

            if (parentOpt.isEmpty()) {
                log.warn("User not found with email: {}", email);
                return ApiResponse.error("User not found or inactive");
            }

            Parent parent = parentOpt.get();
            log.info("Found user: {} with ID: {}", parent.getName(), parent.getId());

            UserProfileResponse response = buildUserProfileResponse(parent);
            log.info("Built profile response for user: {}", parent.getEmail());

            return ApiResponse.success("Profile fetched successfully", response);

        } catch (Exception e) {
            log.error("Error fetching profile for email: {}", email, e);
            return ApiResponse.error("Failed to fetch profile: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<UserProfileResponse> updateProfile(String email, UpdateProfileRequest request) {
        try {
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(email);

            if (parentOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            Parent parent = parentOpt.get();

            // Validate phone number if it's being changed and is different from current
            if (request.getPhone() != null && !request.getPhone().equals(parent.getPhone())) {
                validationUtil.validatePhone(request.getPhone());

                // Check if phone already exists for another user
                if (parentRepository.existsByPhone(request.getPhone())) {
                    return ApiResponse.error("Phone number is already registered by another user");
                }
                parent.setPhone(request.getPhone().trim());
            }

            // Update basic information
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                parent.setName(request.getName().trim());
            }

            if (request.getAge() != null) {
                validationUtil.validateAge(request.getAge());
                parent.setAge(request.getAge());
            }

            // Update additional profile fields
            if (request.getBio() != null) {
                if (request.getBio().length() > 500) {
                    throw new ValidationException("Bio must not exceed 500 characters");
                }
                parent.setBio(request.getBio().trim());
            }

            if (request.getLocation() != null) {
                if (request.getLocation().length() > 100) {
                    throw new ValidationException("Location must not exceed 100 characters");
                }
                parent.setLocation(request.getLocation().trim());
            }

            if (request.getTimezone() != null) {
                parent.setTimezone(request.getTimezone().trim());
            }

            if (request.getNotificationPreferences() != null) {
                parent.setNotificationPreferences(request.getNotificationPreferences());
            }

            Parent savedParent = parentRepository.save(parent);
            UserProfileResponse response = buildUserProfileResponse(savedParent);

            log.info("Profile updated successfully for user: {}", email);
            return ApiResponse.success("Profile updated successfully", response);

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating profile for email: {}", email, e);
            return ApiResponse.error("Failed to update profile");
        }
    }

    @Transactional
    public ApiResponse<ProfileImageResponse> uploadProfileImage(String email, MultipartFile image) {
        try {
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(email);

            if (parentOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            // Validate image file
            validateImageFile(image);

            Parent parent = parentOpt.get();

            // Convert image to Base64
            byte[] imageBytes = image.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String contentType = image.getContentType();

            // Save to database
            parent.setProfileImage(base64Image);
            parent.setProfileImageType(contentType);

            Parent savedParent = parentRepository.save(parent);

            // Build response
            ProfileImageResponse response = ProfileImageResponse.builder()
                    .imageUrl(savedParent.getProfileImageUrl())
                    .uploadedAt(LocalDateTime.now())
                    .build();

            log.info("Profile image uploaded successfully for user: {}", email);
            return ApiResponse.success("Profile image uploaded successfully", response);

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading profile image for email: {}", email, e);
            return ApiResponse.error("Failed to upload profile image");
        }
    }

    @Transactional
    public ApiResponse<Void> deleteProfileImage(String email) {
        try {
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(email);

            if (parentOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            Parent parent = parentOpt.get();
            parent.setProfileImage(null);
            parent.setProfileImageType(null);

            parentRepository.save(parent);

            log.info("Profile image deleted successfully for user: {}", email);
            return ApiResponse.success("Profile image deleted successfully");

        } catch (Exception e) {
            log.error("Error deleting profile image for email: {}", email, e);
            return ApiResponse.error("Failed to delete profile image");
        }
    }

    @Transactional
    public ApiResponse<Void> changePassword(String email, ChangePasswordRequest request) {
        try {
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(email);

            if (parentOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            Parent parent = parentOpt.get();

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), parent.getPassword())) {
                return ApiResponse.error("Current password is incorrect");
            }

            // Validate new password
            validationUtil.validatePassword(request.getNewPassword());

            // Ensure new password is different from current
            if (passwordEncoder.matches(request.getNewPassword(), parent.getPassword())) {
                return ApiResponse.error("New password must be different from current password");
            }

            // Update password
            parent.setPassword(passwordEncoder.encode(request.getNewPassword()));
            parent.setPasswordUpdatedAt(LocalDateTime.now());

            parentRepository.save(parent);

            log.info("Password changed successfully for user: {}", email);
            return ApiResponse.success("Password changed successfully");

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error changing password for email: {}", email, e);
            return ApiResponse.error("Failed to change password");
        }
    }

    public ApiResponse<UserStatsResponse> getUserStats(String email) {
        try {
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(email);

            if (parentOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            Parent parent = parentOpt.get();

            // Calculate days since registration
            long daysSinceRegistration = ChronoUnit.DAYS.between(parent.getCreatedAt().toLocalDate(), LocalDateTime.now().toLocalDate());

            // Calculate days since last login
            long daysSinceLastLogin = parent.getLastLoginAt() != null ?
                    ChronoUnit.DAYS.between(parent.getLastLoginAt().toLocalDate(), LocalDateTime.now().toLocalDate()) : -1;

            UserStatsResponse stats = UserStatsResponse.builder()
                    .daysSinceRegistration((int) daysSinceRegistration)
                    .daysSinceLastLogin(daysSinceLastLogin >= 0 ? (int) daysSinceLastLogin : null)
                    .emailVerified(parent.isEmailVerified())
                    .profileCompleteness(calculateProfileCompleteness(parent))
                    .lastPasswordUpdate(parent.getPasswordUpdatedAt())
                    .accountStatus(parent.isActive() ? "Active" : "Inactive")
                    .totalLogins(getTotalLogins(email)) // This would need to be implemented based on login tracking
                    .build();

            return ApiResponse.success("User statistics fetched successfully", stats);

        } catch (Exception e) {
            log.error("Error fetching user stats for email: {}", email, e);
            return ApiResponse.error("Failed to fetch user statistics");
        }
    }

    private UserProfileResponse buildUserProfileResponse(Parent parent) {
        String profileImageUrl = null;
        try {
            profileImageUrl = parent.getProfileImageUrl();
        } catch (Exception e) {
            log.warn("Error getting profile image URL for user {}: {}", parent.getEmail(), e.getMessage());
            profileImageUrl = null;
        }

        return UserProfileResponse.builder()
                .id(parent.getId())
                .name(parent.getName())
                .email(parent.getEmail())
                .phone(parent.getPhone())
                .age(parent.getAge())
                .bio(parent.getBio())
                .location(parent.getLocation())
                .timezone(parent.getTimezone())
                .profileImageUrl(profileImageUrl)
                .emailVerified(parent.isEmailVerified())
                .active(parent.isActive())
                .createdAt(parent.getCreatedAt())
                .updatedAt(parent.getUpdatedAt())
                .lastLoginAt(parent.getLastLoginAt())
                .memberSince(parent.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                .notificationPreferences(parent.getNotificationPreferences())
                .build();
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

    private int calculateProfileCompleteness(Parent parent) {
        int totalFields = 8; // name, email, phone, age, bio, location, timezone, profileImage
        int completedFields = 0;

        if (parent.getName() != null && !parent.getName().trim().isEmpty()) completedFields++;
        if (parent.getEmail() != null && !parent.getEmail().trim().isEmpty()) completedFields++;
        if (parent.getPhone() != null && !parent.getPhone().trim().isEmpty()) completedFields++;
        if (parent.getAge() != null) completedFields++;
        if (parent.getBio() != null && !parent.getBio().trim().isEmpty()) completedFields++;
        if (parent.getLocation() != null && !parent.getLocation().trim().isEmpty()) completedFields++;
        if (parent.getTimezone() != null && !parent.getTimezone().trim().isEmpty()) completedFields++;
        if (parent.hasProfileImage()) completedFields++;

        return (int) Math.round((double) completedFields / totalFields * 100);
    }

    private int getTotalLogins(String email) {
        // This would typically query a login_attempts table or similar
        // For now, returning a mock value
        // You can implement this by querying the LoginAttempt repository
        return 45; // Mock data
    }
    @Transactional
    public ApiResponse<Void> deleteProfile(String email) {
        try {
            Optional<Parent> parentOpt = parentRepository.findByEmailAndActiveTrue(email);

            if (parentOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            Parent parent = parentOpt.get();

            // Log the deletion for audit purposes
            log.info("Deleting user profile: ID={}, Email={}, Name={}",
                    parent.getId(), parent.getEmail(), parent.getName());

            // Delete the user from database
            parentRepository.delete(parent);

            log.info("User profile deleted successfully for email: {}", email);
            return ApiResponse.success("Profile deleted successfully. Your account has been permanently removed.");

        } catch (Exception e) {
            log.error("Error deleting profile for email: {}", email, e);
            return ApiResponse.error("Failed to delete profile: " + e.getMessage());
        }
    }
}