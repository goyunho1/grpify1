package grpify.grpify.auth;

import grpify.grpify.common.exception.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public abstract class OAuth2UserInfo {

    protected Map<String, Object> attributes;

    /**
     * @param registrationId (application.yml) spring.security.oauth2.registration
     * @param attributes .
     * @return .
     */
    public static OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {

        if (registrationId.equals("spotify")) {
            return new SpotifyOAuth2UserInfo(attributes);
        }
        // 추후 추가
        else if (registrationId.equals("youtube")) {
            return null;
        }
        else {
            throw new NotFoundException("error for " + registrationId);
        }
    }
    // 추상 메서드
    public abstract String getProviderId ();
    public abstract String getProvider ();
    public abstract String getEmail ();
    public abstract String getName ();
    public abstract String getImageUrl ();
}
