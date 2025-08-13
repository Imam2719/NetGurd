package NetGuard.Login_Backend.Repository;

import NetGuard.Login_Backend.Model.PasswordResetToken;
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
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Fixed method signature - returns List instead of Optional
    List<PasswordResetToken> findByEmailAndUsedFalseAndExpiresAtAfter(
            String email, LocalDateTime currentTime);

    // Keep the original method for single token lookup
    Optional<PasswordResetToken> findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, LocalDateTime currentTime);

    @Modifying
    @Transactional
    void deleteByEmailAndUsedFalse(String email);

    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime currentTime);

    // Additional useful methods
    long countByEmailAndUsedFalseAndCreatedAtAfter(String email, LocalDateTime since);

    List<PasswordResetToken> findByEmailOrderByCreatedAtDesc(String email);

    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.createdAt >= :since")
    long countTokensCreatedSince(@Param("since") LocalDateTime since);
}
