package grpify.grpify.spotifyAPI.request.playlist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

public class CreatePlaylist {

    private static final Logger log = LoggerFactory.getLogger(CreatePlaylist.class);

    private final String userId;
    private final String name;
    private final boolean public_;
    private final boolean collaborative;
    private final String description;

    private CreatePlaylist(Builder builder) {
        this.userId = builder.userId;
        this.name = builder.name;
        this.public_ = builder.public_;
        this.collaborative = builder.collaborative;
        this.description = builder.description;
    }

    public String endpoint() {
        return UriComponentsBuilder.fromPath("/users/" + userId + "/playlists")
                .toUriString();
    }

    public String body() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("public", public_);
        body.put("collaborative", collaborative);
        body.put("description", description);

        try {
            return new ObjectMapper().writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

//    public String request(WebClient webClient, String accessToken) { // WebClient 주입 받는 방식 고려.
//        String requestBody = body(); // 요청 바디 저장
//
//        log.info(" [Spotify API Request] URL: {}", endpoint());
//        log.info(" [Spotify API Request] Authorization: Bearer {}", accessToken.substring(0, 10) + "..."); // 토큰 일부만 출력
//        log.info(" [Spotify API Request] Body: {}", requestBody);
//
//        Response response = webClient.post()
//                .uri(endpoint())
//                .headers(headers -> headers.setBearerAuth(accessToken))
//                .bodyValue(requestBody)
//                .retrieve()
//                .bodyToMono(Response.class)
//                .block();
//
//        log.info(" [Spotify API Response] Playlist ID: {}", response.getId());
//
//        return response.getId();
//    }


    public String request(WebClient webClient, String accessToken) { // WebClient 주입 받는 방식 고려.
        return webClient.post()
                .uri(endpoint())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(body())
                .retrieve()
                .bodyToMono(Response.class)
                .block()
                .getId();
    }

    public static class Builder {

        // 필수 파라미터
        private final String userId;
        private String name = "grpify playlist";

        // 선택 파라미터
        private boolean public_ = false;
        private boolean collaborative = false;
        private String description = "";

        public Builder(String userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        public Builder public_(boolean public_) {
            this.public_ = public_;
            return this;
        }

        public Builder collaborative(boolean collaborative) {
            this.collaborative = collaborative;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public CreatePlaylist build() {
            return new CreatePlaylist(this);
        }
    }

    public static class Response {
        @Getter
        @JsonProperty("id")
        private String id;
    }
}