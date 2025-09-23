package grpify.grpify.auth;

import grpify.grpify.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;
@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final User user;                  // 서비스용 User 객체
    private final String provider;            // 선택적으로 provider 구분용
    private final String providerId;          // 선택적으로 providerId 보관

    public CustomOAuth2User(User user,
                            String provider,
                            String providerId,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
    }

}
