package NetGuard.Login_Backend.Service;

import NetGuard.Login_Backend.dto.OAuth2UserInfo;
import NetGuard.Login_Backend.Model.Parent;
import NetGuard.Login_Backend.Repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final ParentRepository parentRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception e) {
            log.error("Error processing OAuth2 user", e);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user");
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = extractUserInfo(registrationId, oauth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Check if user already exists
        Parent existingParent = parentRepository.findByEmail(userInfo.getEmail()).orElse(null);

        if (existingParent != null) {
            // Update existing user's OAuth2 info if needed
            updateExistingParent(existingParent, userInfo);
        }

        // Return OAuth2User with additional attributes
        return new CustomOAuth2User(oauth2User, userInfo);
    }

    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return extractGoogleUserInfo(attributes);
        }
        throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
    }

    private OAuth2UserInfo extractGoogleUserInfo(Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
                .id((String) attributes.get("id"))
                .email((String) attributes.get("email"))
                .name((String) attributes.get("name"))
                .firstName((String) attributes.get("given_name"))
                .lastName((String) attributes.get("family_name"))
                .profilePictureUrl((String) attributes.get("picture"))
                .provider("google")
                .emailVerified(Boolean.TRUE.equals(attributes.get("verified_email")))
                .build();
    }

    private void updateExistingParent(Parent parent, OAuth2UserInfo userInfo) {
        // FIXED: Update profile picture using new fields
        if (userInfo.getProfilePictureUrl() != null && !userInfo.getProfilePictureUrl().isEmpty()) {
            if (!parent.hasProfileImage()) {
                parent.setProfileImage(userInfo.getProfilePictureUrl()); // FIXED: Use new field
                parent.setProfileImageType("image/url"); // FIXED: Use new field with special type for URLs
            }
        }

        // Mark email as verified if OAuth2 provider confirms it
        if (userInfo.isEmailVerified() && !parent.isEmailVerified()) {
            parent.setEmailVerified(true);
            parent.setEmailVerifiedAt(java.time.LocalDateTime.now());
        }

        parentRepository.save(parent);
        log.info("Updated existing parent OAuth2 info for email: {}", userInfo.getEmail());
    }
}