package grpify.grpify.auth;

import grpify.grpify.OAuthAccount.domain.OAuthAccount;
import grpify.grpify.OAuthAccount.repository.OAuthAccountRepository;
import grpify.grpify.user.domain.User;
import grpify.grpify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;


@Service
@RequiredArgsConstructor

public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    /**
     * OAuth2 제공자로부터 Access Token을 받은 후 호출되는 메서드.
     * 이 메서드에서 사용자 정보를 가져와 DB에 저장하거나 업데이트한다.
     * @param userRequest OAuth2 제공자, Access Token 등의 정보를 담고 있음
     * @return 인증 정보를 담은 OAuth2User 객체 (우리는 CustomOAuth2User 사용)
     *
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. Spring Security의 기본 로직을 통해 OAuth2User 객체를 가져온다.
        //    이 객체는 제공자로부터 받은 사용자 속성(attributes)을 담고 있다.
        OAuth2User oAuth2User = super.loadUser(userRequest);
        // 2. 어떤 OAuth2 제공자인지 registrationId를 통해 확인한다. (예: "spotify", "google")
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        //                                             registrationId 에 맞춰 attribute 파싱해서 OAuth2UserInfo 생성
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.from(registrationId, oAuth2User.getAttributes());

        String providerAccessToken = userRequest.getAccessToken().getTokenValue();
        String providerRefreshToken = (String) userRequest.getAdditionalParameters().get("refresh_token");

        OAuthAccount oAuthAccount = oAuthAccountRepository
                .findByProviderAndProviderId(oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId())
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(oAuth2UserInfo.getEmail())
                            .orElseGet(() -> userRepository.save(User.builder()
                                    .name(oAuth2UserInfo.getName())
                                    .email(oAuth2UserInfo.getEmail())
                                    .profileImgUrl(oAuth2UserInfo.getImageUrl())
                                    .build()));

                    return oAuthAccountRepository.save(OAuthAccount.builder()
                            .user(user)
                            .provider(oAuth2UserInfo.getProvider())
                            .providerId(oAuth2UserInfo.getProviderId())
                            .accessToken(providerAccessToken)
                            .refreshToken(providerRefreshToken)
                            .tokenExpiry(userRequest.getAccessToken().getExpiresAt())
                            .build());
                });

        // 기존 OAuthAccount 업데이트 (재로그인 시 토큰 갱신)
        oAuthAccount.setAccessToken(providerAccessToken);
        // 최초 로그인 시에만 값이 존재
        if (providerRefreshToken != null) {
            oAuthAccount.setRefreshToken(providerRefreshToken);
        }
        oAuthAccount.setTokenExpiry(userRequest.getAccessToken().getExpiresAt());

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new CustomOAuth2User(
                oAuthAccount.getUser(),                      // User 객체
                oAuth2UserInfo.getProvider(),                // provider
                oAuth2UserInfo.getProviderId(),              // providerId
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), // 권한
                oAuth2User.getAttributes(),                  // OAuth2 제공자 속성
                userNameAttributeName                         // 이름 키
        );
    }
}
