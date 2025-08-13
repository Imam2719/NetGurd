package NetGuard.Login_Backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret:mySecretKey}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;

    private static final long OAUTH2_TEMP_EXPIRATION = 600000; // 10 minutes in milliseconds

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // =====================================
    // STANDARD JWT TOKEN METHODS
    // =====================================

    /**
     * Generate JWT token with email and name only (legacy method)
     */
    public String generateToken(String email, String name) {
        return Jwts.builder()
                .setSubject(email)
                .claim("name", name)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate JWT token with email, name, and userId (primary method used by AuthService)
     */
    public String generateToken(String email, String name, Long userId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("name", name)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract email from JWT token
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting email from token", e);
            return null;
        }
    }

    /**
     * Extract user ID from JWT token
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.error("Error extracting userId from token", e);
            return null;
        }
    }

    /**
     * Extract name from JWT token
     */
    public String getNameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("name", String.class);
        } catch (Exception e) {
            log.error("Error extracting name from token", e);
            return null;
        }
    }

    /**
     * Check if JWT token is valid (not expired and properly signed)
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if JWT token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.debug("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Get token expiration time in milliseconds
     */
    public Long getExpirationTime() {
        return expiration;
    }

    // =====================================
    // OAUTH2 TEMPORARY TOKEN METHODS
    // =====================================

    /**
     * Generate temporary OAuth2 token with profile picture URL support
     * This is the primary OAuth2 token generation method
     */
    public String generateTemporaryOAuth2TokenWithPicture(String email, String name, String profilePictureUrl) {
        try {
            JwtBuilder builder = Jwts.builder()
                    .setSubject(email)
                    .claim("name", name)
                    .claim("type", "oauth2_temp")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + OAUTH2_TEMP_EXPIRATION))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256);

            // Add profile picture URL if available
            if (profilePictureUrl != null && !profilePictureUrl.trim().isEmpty()) {
                builder.claim("profilePictureUrl", profilePictureUrl);
                log.debug("Added profile picture URL to OAuth2 token for user: {}", email);
            }

            return builder.compact();
        } catch (Exception e) {
            log.error("Error generating OAuth2 token with picture for email: {}", email, e);
            throw new RuntimeException("Failed to generate OAuth2 token", e);
        }
    }

    /**
     * Generate temporary OAuth2 token without profile picture (fallback method)
     */
    public String generateTemporaryOAuth2Token(String email, String name) {
        return generateTemporaryOAuth2TokenWithPicture(email, name, null);
    }

    /**
     * Extract profile picture URL from OAuth2 token
     */
    public String getProfilePictureUrlFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String profilePictureUrl = claims.get("profilePictureUrl", String.class);
            log.debug("Extracted profile picture URL from token: {}", profilePictureUrl != null ? "present" : "not present");
            return profilePictureUrl;
        } catch (Exception e) {
            log.debug("Error extracting profile picture URL from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is a temporary OAuth2 token
     */
    public boolean isTemporaryOAuth2Token(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return "oauth2_temp".equals(claims.get("type"));
        } catch (Exception e) {
            log.debug("Error checking OAuth2 token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if OAuth2 token is valid (both properly formed and marked as OAuth2 temporary)
     */
    public boolean isOAuth2TokenValid(String token) {
        return isTokenValid(token) && isTemporaryOAuth2Token(token);
    }

    // =====================================
    // UTILITY METHODS
    // =====================================

    /**
     * Extract all claims from a token for debugging purposes
     */
    public Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error extracting claims from token", e);
            return null;
        }
    }

    /**
     * Get remaining time until token expiration in milliseconds
     */
    public Long getRemainingTimeFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            if (claims != null) {
                Date expiration = claims.getExpiration();
                Date now = new Date();
                return expiration.getTime() - now.getTime();
            }
        } catch (Exception e) {
            log.debug("Error calculating remaining time from token: {}", e.getMessage());
        }
        return 0L;
    }

    /**
     * Check if token will expire within specified minutes
     */
    public boolean willExpireWithin(String token, int minutes) {
        Long remainingTime = getRemainingTimeFromToken(token);
        return remainingTime != null && remainingTime < (minutes * 60 * 1000);
    }

    /**
     * Get token type (regular or oauth2_temp)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            if (claims != null) {
                String type = claims.get("type", String.class);
                return type != null ? type : "regular";
            }
        } catch (Exception e) {
            log.debug("Error getting token type: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * Validate token and return detailed information
     */
    public TokenInfo validateTokenAndGetInfo(String token) {
        try {
            if (!isTokenValid(token)) {
                return TokenInfo.invalid("Token is invalid or malformed");
            }

            Claims claims = getAllClaimsFromToken(token);
            if (claims == null) {
                return TokenInfo.invalid("Unable to extract claims from token");
            }

            String email = claims.getSubject();
            String name = claims.get("name", String.class);
            String type = claims.get("type", String.class);
            Long userId = claims.get("userId", Long.class);
            String profilePictureUrl = claims.get("profilePictureUrl", String.class);
            Date expiration = claims.getExpiration();
            boolean isExpired = expiration.before(new Date());

            if (isExpired) {
                return TokenInfo.invalid("Token has expired");
            }

            return TokenInfo.valid(email, name, userId, type, profilePictureUrl, expiration);

        } catch (Exception e) {
            log.error("Error validating token", e);
            return TokenInfo.invalid("Token validation failed: " + e.getMessage());
        }
    }

    // =====================================
    // INNER CLASS FOR TOKEN INFO
    // =====================================

    public static class TokenInfo {
        private final boolean valid;
        private final String email;
        private final String name;
        private final Long userId;
        private final String type;
        private final String profilePictureUrl;
        private final Date expiration;
        private final String errorMessage;

        private TokenInfo(boolean valid, String email, String name, Long userId, String type,
                          String profilePictureUrl, Date expiration, String errorMessage) {
            this.valid = valid;
            this.email = email;
            this.name = name;
            this.userId = userId;
            this.type = type;
            this.profilePictureUrl = profilePictureUrl;
            this.expiration = expiration;
            this.errorMessage = errorMessage;
        }

        public static TokenInfo valid(String email, String name, Long userId, String type,
                                      String profilePictureUrl, Date expiration) {
            return new TokenInfo(true, email, name, userId, type, profilePictureUrl, expiration, null);
        }

        public static TokenInfo invalid(String errorMessage) {
            return new TokenInfo(false, null, null, null, null, null, null, errorMessage);
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public Long getUserId() { return userId; }
        public String getType() { return type; }
        public String getProfilePictureUrl() { return profilePictureUrl; }
        public Date getExpiration() { return expiration; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isOAuth2Token() { return "oauth2_temp".equals(type); }
        public boolean hasProfilePicture() { return profilePictureUrl != null && !profilePictureUrl.trim().isEmpty(); }
    }
}