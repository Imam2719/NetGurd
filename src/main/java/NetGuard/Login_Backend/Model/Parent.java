package NetGuard.Login_Backend.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "parents", indexes = {
        @Index(name = "idx_parent_email", columnList = "email"),
        @Index(name = "idx_parent_phone", columnList = "phone"),
        @Index(name = "idx_parent_active", columnList = "active"),
        @Index(name = "idx_parent_email_verified", columnList = "email_verified"),
        @Index(name = "idx_parent_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "age")
    private Integer age;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;

    @Column(name = "profile_image_type", length = 50)
    private String profileImageType; // MIME type (image/jpeg, image/png, etc.)

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "registration_ip", length = 45)
    private String registrationIp;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "privacy_accepted_at")
    private LocalDateTime privacyAcceptedAt;

    // Additional profile fields
    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "notification_preferences", length = 500)
    private String notificationPreferences; // JSON string for notification settings

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Helper methods
    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void lockAccount(int lockoutDurationMinutes) {
        this.accountLockedUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
    }

    // Business logic methods
    public String getDisplayName() {
        return name != null && !name.trim().isEmpty() ? name : email;
    }

    public String getProfileImageUrl() {
        if (profileImage == null || profileImage.trim().isEmpty()) {
            return null;
        }

        // If it's a URL (from OAuth2), return as is
        if ("image/url".equals(profileImageType) || profileImage.startsWith("http")) {
            return profileImage;
        }

        // If it's Base64 data, return as data URL
        try {
            return "data:" + (profileImageType != null ? profileImageType : "image/jpeg") + ";base64," + profileImage;
        } catch (Exception e) {
            // Handle any potential issues with malformed data
            return null;
        }
    }

    public boolean hasProfileImage() {
        return profileImage != null && !profileImage.trim().isEmpty();
    }

    @PrePersist
    protected void prePersist() {
        if (passwordUpdatedAt == null) {
            passwordUpdatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void preUpdate() {
        // Additional business logic can be added here
    }
}