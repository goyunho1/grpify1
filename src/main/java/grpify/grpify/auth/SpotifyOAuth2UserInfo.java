package grpify.grpify.auth;

import java.util.List;
import java.util.Map;

public class SpotifyOAuth2UserInfo extends OAuth2UserInfo {
    public SpotifyOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }


    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getProvider() {
        return "spotify";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("display_name");
    }

    @Override
    public String getImageUrl() {
        List<Map<String, Object>> images = (List<Map<String, Object>>) attributes.get("images");

        if (images == null || images.isEmpty()) {
            return null;
        }
        return (String) images.get(0).get("url");
    }
}
