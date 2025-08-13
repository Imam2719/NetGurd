package NetGuard.Login_Backend.task;

import NetGuard.Login_Backend.Service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {

    private final AuthService authService;

    // Run every hour to cleanup expired tokens
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        log.info("Running token cleanup task");
        authService.cleanupExpiredTokens();
    }
}
