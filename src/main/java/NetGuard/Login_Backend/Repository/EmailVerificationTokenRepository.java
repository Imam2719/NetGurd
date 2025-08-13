package NetGuard.Login_Backend.Repository;

import NetGuard.Login_Backend.Model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Find a valid verification token that hasn't been used and hasn't expired
     */
    Optional<EmailVerificationToken> findByTokenAndUsedFalseAndExpiresAtAfter(
            String token, LocalDateTime currentTime);

    /**
     * Find all verification tokens for a specific email address
     */
    List<EmailVerificationToken> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Find the most recent verification token for an email address
     */
    Optional<EmailVerificationToken> findTopByEmailOrderByCreatedAtDesc(String email);

    /**
     * Find all unused tokens for a specific email address
     */
    List<EmailVerificationToken> findByEmailAndUsedFalse(String email);

    /**
     * Check if a verification token exists and is valid
     */
    boolean existsByTokenAndUsedFalseAndExpiresAtAfter(String token, LocalDateTime currentTime);

    /**
     * Check if there are any unused tokens for an email address
     */
    boolean existsByEmailAndUsedFalse(String email);

    /**
     * Count unused tokens for a specific email (useful for rate limiting)
     */
    long countByEmailAndUsedFalseAndCreatedAtAfter(String email, LocalDateTime since);

    /**
     * Delete all unused tokens for a specific email address
     * This is useful when creating a new token to prevent multiple active tokens
     */
    @Modifying
    @Transactional
    void deleteByEmailAndUsedFalse(String email);

    /**
     * Delete all expired tokens regardless of used status
     * This method should be called periodically for cleanup
     */
    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime currentTime);

    /**
     * Delete old used tokens (cleanup method)
     * Keep used tokens for a reasonable period for audit purposes
     */
    @Modifying
    @Transactional
    void deleteByUsedTrueAndUsedAtBefore(LocalDateTime cutoffTime);

    /**
     * Find tokens that will expire soon (useful for notifications or cleanup warnings)
     */
    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.used = false AND evt.expiresAt BETWEEN :now AND :warningTime")
    List<EmailVerificationToken> findTokensExpiringSoon(
            @Param("now") LocalDateTime now,
            @Param("warningTime") LocalDateTime warningTime);

    /**
     * Get verification statistics for monitoring
     */
    @Query("SELECT COUNT(evt) FROM EmailVerificationToken evt WHERE evt.createdAt >= :since")
    long countTokensCreatedSince(@Param("since") LocalDateTime since);

    /**
     * Get usage statistics for monitoring
     */
    @Query("SELECT COUNT(evt) FROM EmailVerificationToken evt WHERE evt.used = true AND evt.usedAt >= :since")
    long countTokensUsedSince(@Param("since") LocalDateTime since);

    /**
     * Find tokens by email with pagination support
     */
    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.email = :email ORDER BY evt.createdAt DESC")
    List<EmailVerificationToken> findByEmailWithLimit(@Param("email") String email,
                                                      org.springframework.data.domain.Pageable pageable);

    /**
     * Batch update to mark expired tokens as invalid (alternative to deletion)
     * This preserves tokens for audit while making them unusable
     */
    @Modifying
    @Transactional
    @Query("UPDATE EmailVerificationToken evt SET evt.used = true WHERE evt.expiresAt < :currentTime AND evt.used = false")
    int markExpiredTokensAsUsed(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Custom query to find tokens that need attention (expired but not cleaned up)
     */
    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.used = false AND evt.expiresAt < :currentTime")
    List<EmailVerificationToken> findExpiredUnusedTokens(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Security method to detect potential abuse
     * Find emails with excessive token generation in a short period
     */
    @Query("SELECT evt.email, COUNT(evt) FROM EmailVerificationToken evt " +
            "WHERE evt.createdAt >= :since " +
            "GROUP BY evt.email " +
            "HAVING COUNT(evt) > :threshold")
    List<Object[]> findEmailsWithExcessiveTokenGeneration(
            @Param("since") LocalDateTime since,
            @Param("threshold") long threshold);
}