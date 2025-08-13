package NetGuard.Login_Backend.Service;

import NetGuard.Login_Backend.Repository.LoginAttemptRepository;
import NetGuard.Login_Backend.Repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final ParentRepository parentRepository;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${app.security.rate-limit-window-minutes:15}")
    private int rateLimitWindowMinutes;

    public boolean isAccountLocked(String email, String ipAddress) {
        // Check database-level account lock
        boolean isDbLocked = parentRepository.findByEmail(email)
                .map(parent -> parent.isAccountLocked())
                .orElse(false);

        if (isDbLocked) {
            return true;
        }

        // Check recent failed attempts
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);
        long recentFailedAttempts = loginAttemptRepository
                .countByEmailAndIpAddressAndSuccessfulFalseAndAttemptTimeAfter(
                        email, ipAddress, windowStart);

        return recentFailedAttempts >= maxLoginAttempts;
    }

    public void lockAccountIfNeeded(String email) {
        parentRepository.findByEmail(email).ifPresent(parent -> {
            parent.incrementFailedLoginAttempts();

            if (parent.getFailedLoginAttempts() >= maxLoginAttempts) {
                parent.lockAccount(lockoutDurationMinutes);
                log.warn("Account locked for email: {} due to {} failed attempts",
                        email, parent.getFailedLoginAttempts());
            }

            parentRepository.save(parent);
        });
    }

    public void resetAccountLockout(String email) {
        parentRepository.findByEmail(email).ifPresent(parent -> {
            parent.resetFailedLoginAttempts();
            parentRepository.save(parent);
        });
    }
}
