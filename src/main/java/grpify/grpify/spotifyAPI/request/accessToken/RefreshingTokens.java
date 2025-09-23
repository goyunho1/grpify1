package grpify.grpify.spotifyAPI.request.accessToken;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RefreshingTokens {
    private final String refreshToken;
    private final String clientId;
    private final String clientSecret;

    // 빌더를 통해서만 생성 가능하도록 private 생성자
    private RefreshingTokens(Builder builder) {
        this.refreshToken = builder.refreshToken;
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
    }

    public String endpoint() {
        return "https://accounts.spotify.com/api/token";
    }

//    public String body() {
//        Map<String, Object> body = new HashMap<>();
//        body.put("grant_type", "refresh_token");
//        body.put("refresh_token", refreshToken);
//
//        try {
//            return new ObjectMapper().writeValueAsString(body);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException("Failed to serialize request body", e);
//        }
//    }
    //json 형식 x
    //application/x-www-form-urlencoded 형식으로 작성해야 됨
    //key=value&key2=value2 형태

    public Response request(WebClient webClient) {
        // "Authorization": "Basic <base64 encoded client_id:client_secret>"
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        return webClient.post()
                .uri(endpoint())
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", this.refreshToken))
                .retrieve()
                .bodyToMono(Response.class)
                .block(); // 비동기 작업을 동기적으로 기다려서 결과를 받음
    }

    public static class Builder {
        private String refreshToken;
        private String clientId;
        private String clientSecret;

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public RefreshingTokens build() {
            return new RefreshingTokens(this);
        }
    }

    @Getter
    public static class Response {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("scope")
        private String scope;

//        The refresh token contained in the response,
//        can be used to request new tokens.
//        Depending on the grant used to get the initial refresh token,
//        a refresh token might not be included in each response.
//        When a refresh token is not returned, continue using the existing token.
        // 분기 처리 필요
        @JsonProperty("refresh_token")
        private String refreshToken;
    }
}
