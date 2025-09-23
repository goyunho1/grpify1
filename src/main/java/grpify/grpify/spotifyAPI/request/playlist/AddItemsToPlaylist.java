package groupifyApi.request.playlist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddItemsToPlaylist {

    private final String playlistId;
    private final List<String> uris;
    private final Integer position;

    private AddItemsToPlaylist(Builder builder) {
        this.playlistId = builder.playlistId;
        this.uris = builder.uris;
        this.position = builder.position;
    }

    public String endpoint() {
        return UriComponentsBuilder.fromPath("/playlists/" + playlistId + "/tracks")
                .toUriString();
    }

    public String body() {
        Map<String, Object> body = new HashMap<>();
        body.put("uris", uris);
        body.put("position", position);

        try {
            return new ObjectMapper().writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    public void request(WebClient webClient, String accessToken) { // WebClient 주입 받는 방식 고려.
        webClient.post()
            .uri(endpoint())
            .headers(headers -> headers.setBearerAuth(accessToken))
            .bodyValue(body())
            .retrieve()
            .toBodilessEntity()  // 상태 코드만 받아옴
            .block();
    }

    public static final class Builder {

        // 필수 파라미터
        private final String playlistId;
        private List<String> uris;

        // 선택 파라미터
        private Integer position = 0;

        public Builder(String playlistId, List<String> uris) {
            this.playlistId = playlistId;
            this.uris = uris;
        }

        public Builder position(Integer position) {
            this.position = position;
            return this;
        }

        public AddItemsToPlaylist build() {
            return new AddItemsToPlaylist(this);
        }
    }

//    public static class Response {
//        private
//    }
}
