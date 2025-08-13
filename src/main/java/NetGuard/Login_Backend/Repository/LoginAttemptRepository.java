package NetGuard.Login_Backend.Repository;

import NetGuard.Login_Backend.Model.LoginAttempt;
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
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Count failed login attempts for a specific email and IP address within a time window
     * This method is critical for account lockout and rate limiting functionality
     */
    long countByEmailAndIpAddressAndSuccessfulFalseAndAttemptTimeAfter(
            String email, String ipAddress, LocalDateTime since);

    /**
     * Count failed login attempts for a specific email regardless of IP address
     * Used for email-based rate limiting and security monitoring
     */
    long countByEmailAndSuccessfulFalseAndAttemptTimeAfter(
            String email, LocalDateTime since);

    /**
     * Count failed login attempts from a specific IP address regardless of email
     * Used for IP-based rate limiting and bot detection
     */
    long countByIpAddressAndSuccessfulFalseAndAttemptTimeAfter(
            String ipAddress, LocalDateTime since);

    /**
     * Find all login attempts for a specific email address ordered by most recent
     * Used for audit trails and security analysis
     */
    List<LoginAttempt> findByEmailOrderByAttemptTimeDesc(String email);

    /**
     * Find recent login attempts for a specific email with limit
     * Optimized for dashboard displays and recent activity monitoring
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.email = :email ORDER BY la.attemptTime DESC")
    List<LoginAttempt> findRecentAttemptsByEmail(@Param("email") String email,
                                                 org.springframework.data.domain.Pageable pageable);

    /**
     * Find all login attempts from a specific IP address
     * Used for security analysis and potential threat identification
     */
    List<LoginAttempt> findByIpAddressOrderByAttemptTimeDesc(String ipAddress);

    /**
     * Find the most recent successful login for an email address
     * Used for last login tracking and security notifications
     */
    Optional<LoginAttempt> findTopByEmailAndSuccessfulTrueOrderByAttemptTimeDesc(String email);

    /**
     * Find the most recent login attempt (successful or failed) for an email
     * Used for comprehensive login tracking
     */
    Optional<LoginAttempt> findTopByEmailOrderByAttemptTimeDesc(String email);

    /**
     * Find failed login attempts within a specific time range for security monitoring
     */
    List<LoginAttempt> findBySuccessfulFalseAndAttemptTimeBetweenOrderByAttemptTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find successful login attempts within a specific time range
     * Used for activity monitoring and compliance reporting
     */
    List<LoginAttempt> findBySuccessfulTrueAndAttemptTimeBetweenOrderByAttemptTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Delete old login attempts for data retention compliance
     * Should be called periodically to maintain database performance
     */
    @Modifying
    @Transactional
    void deleteByAttemptTimeBefore(LocalDateTime cutoffTime);

    /**
     * Count total login attempts within a specific time period
     * Used for system monitoring and capacity planning
     */
    long countByAttemptTimeAfter(LocalDateTime since);

    /**
     * Count successful login attempts within a specific time period
     * Used for success rate monitoring and analytics
     */
    long countBySuccessfulTrueAndAttemptTimeAfter(LocalDateTime since);

    /**
     * Count failed login attempts within a specific time period
     * Used for security monitoring and threat analysis
     */
    long countBySuccessfulFalseAndAttemptTimeAfter(LocalDateTime since);

    /**
     * Find IP addresses with excessive failed login attempts
     * Used for automated threat detection and IP blocking
     */
    @Query("SELECT la.ipAddress, COUNT(la) FROM LoginAttempt la " +
            "WHERE la.successful = false AND la.attemptTime >= :since " +
            "GROUP BY la.ipAddress " +
            "HAVING COUNT(la) > :threshold " +
            "ORDER BY COUNT(la) DESC")
    List<Object[]> findIpAddressesWithExcessiveFailedAttempts(
            @Param("since") LocalDateTime since,
            @Param("threshold") long threshold);

    /**
     * Find emails with excessive failed login attempts
     * Used for account security monitoring and potential compromise detection
     */
    @Query("SELECT la.email, COUNT(la) FROM LoginAttempt la " +
            "WHERE la.successful = false AND la.attemptTime >= :since " +
            "GROUP BY la.email " +
            "HAVING COUNT(la) > :threshold " +
            "ORDER BY COUNT(la) DESC")
    List<Object[]> findEmailsWithExcessiveFailedAttempts(
            @Param("since") LocalDateTime since,
            @Param("threshold") long threshold);

    /**
     * Find distinct IP addresses that have attempted to log in with multiple different emails
     * Used for detecting credential stuffing attacks
     */
    @Query("SELECT la.ipAddress, COUNT(DISTINCT la.email) FROM LoginAttempt la " +
            "WHERE la.attemptTime >= :since " +
            "GROUP BY la.ipAddress " +
            "HAVING COUNT(DISTINCT la.email) > :threshold " +
            "ORDER BY COUNT(DISTINCT la.email) DESC")
    List<Object[]> findIpAddressesWithMultipleEmailAttempts(
            @Param("since") LocalDateTime since,
            @Param("threshold") long threshold);

    /**
     * Find login attempts with specific failure reasons for analysis
     * Used for identifying common authentication issues
     */
    List<LoginAttempt> findByFailureReasonAndAttemptTimeAfterOrderByAttemptTimeDesc(
            String failureReason, LocalDateTime since);

    /**
     * Get login attempt statistics for a specific email address
     * Used for user account security dashboards
     */
    @Query("SELECT " +
            "COUNT(CASE WHEN la.successful = true THEN 1 END) as successfulAttempts, " +
            "COUNT(CASE WHEN la.successful = false THEN 1 END) as failedAttempts, " +
            "COUNT(DISTINCT la.ipAddress) as distinctIpAddresses " +
            "FROM LoginAttempt la " +
            "WHERE la.email = :email AND la.attemptTime >= :since")
    Object[] getLoginStatisticsForEmail(@Param("email") String email, @Param("since") LocalDateTime since);

    /**
     * Get system-wide login attempt statistics
     * Used for administrative dashboards and system monitoring
     */
    @Query("SELECT " +
            "COUNT(CASE WHEN la.successful = true THEN 1 END) as successfulAttempts, " +
            "COUNT(CASE WHEN la.successful = false THEN 1 END) as failedAttempts, " +
            "COUNT(DISTINCT la.email) as distinctEmails, " +
            "COUNT(DISTINCT la.ipAddress) as distinctIpAddresses " +
            "FROM LoginAttempt la " +
            "WHERE la.attemptTime >= :since")
    Object[] getSystemLoginStatistics(@Param("since") LocalDateTime since);

    /**
     * Find recent failed attempts for a specific email and IP combination
     * Used for progressive rate limiting and security analysis
     */
    List<LoginAttempt> findByEmailAndIpAddressAndSuccessfulFalseAndAttemptTimeAfterOrderByAttemptTimeDesc(
            String email, String ipAddress, LocalDateTime since);

    /**
     * Check if there are any successful logins for an email from a specific IP
     * Used for trusted IP detection and risk assessment
     */
    boolean existsByEmailAndIpAddressAndSuccessfulTrue(String email, String ipAddress);

    /**
     * Find the first successful login from a specific IP for an email
     * Used for IP reputation and trust scoring
     */
    Optional<LoginAttempt> findTopByEmailAndIpAddressAndSuccessfulTrueOrderByAttemptTimeAsc(
            String email, String ipAddress);

    /**
     * Batch update old login attempts to reduce storage impact
     * Alternative to deletion for compliance requirements
     */
    @Modifying
    @Transactional
    @Query("UPDATE LoginAttempt la SET la.userAgent = 'ARCHIVED' WHERE la.attemptTime < :cutoffTime AND la.userAgent IS NOT NULL")
    int archiveOldLoginAttempts(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count unique users who have logged in successfully within a time period
     * Used for active user metrics and business analytics
     */
    @Query("SELECT COUNT(DISTINCT la.email) FROM LoginAttempt la WHERE la.successful = true AND la.attemptTime >= :since")
    long countDistinctSuccessfulUsers(@Param("since") LocalDateTime since);
}