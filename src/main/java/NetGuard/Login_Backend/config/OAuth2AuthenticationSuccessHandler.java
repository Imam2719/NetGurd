package NetGuard.Login_Backend.config;

import NetGuard.Login_Backend.Service.CustomOAuth2User;
import NetGuard.Login_Backend.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.authorized-redirect-uris}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {

        // Extract user information from either OidcUser or CustomOAuth2User
        String email;
        String name;
        String profilePictureUrl;
        String provider = "google"; // Default to google
        boolean emailVerified = true; // Default to true for Google

        Object principal = authentication.getPrincipal();

        if (principal instanceof OidcUser oidcUser) {
            // Handle OpenID Connect user (Google with openid scope)
            email = oidcUser.getEmail();
            name = oidcUser.getFullName();
            profilePictureUrl = oidcUser.getAttribute("picture");
            emailVerified = oidcUser.getEmailVerified() != null ? oidcUser.getEmailVerified() : true;

            log.debug("Processing OidcUser: email={}, name={}, picture={}", email, name, profilePictureUrl);

        } else if (principal instanceof CustomOAuth2User oauth2User) {
            // Handle custom OAuth2 user
            email = oauth2User.getUserInfo().getEmail();
            name = oauth2User.getUserInfo().getName();
            profilePictureUrl = oauth2User.getUserInfo().getProfilePictureUrl();
            provider = oauth2User.getUserInfo().getProvider();
            emailVerified = oauth2User.getUserInfo().isEmailVerified();

            log.debug("Processing CustomOAuth2User: email={}, name={}, picture={}", email, name, profilePictureUrl);

        } else if (principal instanceof OAuth2User oauth2User) {
            // Handle generic OAuth2 user as fallback
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            profilePictureUrl = oauth2User.getAttribute("picture");

            log.debug("Processing generic OAuth2User: email={}, name={}, picture={}", email, name, profilePictureUrl);

        } else {
            log.error("Unknown principal type: {}", principal.getClass().getName());
            throw new IllegalStateException("Unknown principal type: " + principal.getClass().getName());
        }

        // Validate required fields
        if (email == null || email.trim().isEmpty()) {
            log.error("Email is null or empty for user: {}", name);
            throw new IllegalStateException("Email cannot be null or empty");
        }

        if (name == null || name.trim().isEmpty()) {
            name = email; // Use email as fallback name
        }

        // FIXED: Create temporary token for OAuth2 flow with profile picture URL
        String tempToken = jwtUtil.generateTemporaryOAuth2TokenWithPicture(email, name, profilePictureUrl);

        // Encode user information for frontend
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String encodedPicture = profilePictureUrl != null
                ? URLEncoder.encode(profilePictureUrl, StandardCharsets.UTF_8)
                : "";

        log.debug("Creating redirect URL with email={}, name={}, provider={}, picture={}",
                encodedEmail, encodedName, provider, profilePictureUrl);

        return UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                .queryParam("token", tempToken)
                .queryParam("email", encodedEmail)
                .queryParam("name", encodedName)
                .queryParam("picture", encodedPicture)
                .queryParam("provider", provider)
                .queryParam("emailVerified", emailVerified)
                .build().toUriString();
    }
}