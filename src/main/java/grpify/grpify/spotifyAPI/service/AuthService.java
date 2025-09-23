package groupifyApi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.spotify.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.spotify.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.spotify.scope}")
    private String scope;

    public AuthService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://accounts.spotify.com/api").build();
    }

    /**
     * Spotify OAuth 인증 URL을 생성하여 반환
     */
    public String getAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString("https://accounts.spotify.com/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .toUriString();
    }

    /**
     * 사용자의 인증 코드를 받아 Spotify에서 Access Token을 요청하는 메서드
     */
    public String getAccessToken(String authorizationCode) {
        return webClient.post()
                .uri("/token")
                .headers(headers -> {
                    headers.setBasicAuth(clientId, clientSecret);
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                })
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("code", authorizationCode)
                        .with("redirect_uri", redirectUri))
                .retrieve()
                .bodyToMono(Map.class) // JSON 응답을 Map 형태로 변환 (비동기 처리)
                .map(response -> (String) response.get("access_token"))// 응답에서 "access_token" 값 추출 후 반환
                .block();
    }
}
