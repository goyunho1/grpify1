package grpify.grpify.auth;

import grpify.grpify.OAuthAccount.domain.OAuthAccount;
import grpify.grpify.OAuthAccount.repository.OAuthAccountRepository;
import grpify.grpify.common.exception.NotFoundException;
import grpify.grpify.spotifyAPI.request.accessToken.RefreshingTokens;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SpotifyTokenService {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyTokenService.class);

    private final WebClient webClient;
    private final OAuthAccountRepository oAuthAccountRepository;

    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String spotifyClientId;

    @Value("${spring.security.oauth2.client.registration.spotify.client-secret}")
    private String spotifyClientSecret;

    @Transactional
    public String getValidAccessToken(Long userId) {
        OAuthAccount account = oAuthAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("OAuthAccount not found"));

        String token = account.getAccessToken();

        if (isExpired(account.getTokenExpiry())) {
            token = refreshAccessToken(account);
        }

        return token;
    }

    private boolean isExpired(Instant expiry) {
        return expiry == null || expiry.isBefore(Instant.now());
    }

//    @Transactional
    //getValidAccessToken 에서 이미 사용, getValidAccessToken만 public 허용
//    customoauth2userService에서
//
//oAuthAccount.setAccessToken(providerAccessToken);
//    로 갱신을 해주지만 우리가 SpotifyTokenService 작성해서 토큰 갱신하는 이유는 로그인 한 후 시간이 많이 지나서 갱신이 필요할 수도 있기 때문
    private String refreshAccessToken(OAuthAccount account) {
        if (account.getRefreshToken() == null) {
            throw new IllegalStateException("Refresh token not available. Please re-authenticate.");
        }

        logger.info("Refreshing access token for user affiliated with account ID: {}", account.getId());

        try {
            // 1. 빌더를 사용하여 토큰 갱신 요청(명령) 객체 생성
            RefreshingTokens request = new RefreshingTokens.Builder()
                    .refreshToken(account.getRefreshToken())
                    .clientId(spotifyClientId)
                    .clientSecret(spotifyClientSecret)
                    .build();

            // 2. 요청 객체의 request 메서드를 호출하여 API 통신 실행
            RefreshingTokens.Response response = request.request(webClient);

            // 3. 응답(DTO)을 바탕으로 엔터티 정보 업데이트
            account.setAccessToken(response.getAccessToken());
            account.setTokenExpiry(Instant.now().plusSeconds(response.getExpiresIn()));

            // Spotify가 새로운 Refresh Token을 줬다면, 그것으로 업데이트
            if (response.getRefreshToken() != null) {
                account.setRefreshToken(response.getRefreshToken());
            }

            logger.info("Successfully refreshed access token for account ID: {}", account.getId());

            return response.getAccessToken();

        } catch (Exception e) {
            logger.error("Error while refreshing access token for account ID: {}", account.getId(), e);
            throw new RuntimeException("Could not refresh access token.", e);
        }
    }
}



