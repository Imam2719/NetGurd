package NetGuard.Login_Backend.Service;

import NetGuard.Login_Backend.dto.OAuth2UserInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;


public class CustomOAuth2User implements OAuth2User {
    private final OAuth2User oauth2User;
    private final OAuth2UserInfo userInfo;

    public CustomOAuth2User(OAuth2User oauth2User, OAuth2UserInfo userInfo) {
        this.oauth2User = oauth2User;
        this.userInfo = userInfo;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2User.getAttribute("name");
    }

    public OAuth2UserInfo getUserInfo() {
        return userInfo;
    }
}
