package grpify.grpify.spotifyAPI.request.users;


import grpify.grpify.spotifyAPI.request.dto.GetUsersTopTracksResponse;
import grpify.grpify.spotifyAPI.request.dto.Track;

import grpify.grpify.spotifyAPI.request.dto.GetUsersTopTracksResponse;
import grpify.grpify.spotifyAPI.request.dto.Track;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class GetUsersTopTracks {

    private final String time_range;
    private final int limit;

    private GetUsersTopTracks(Builder builder) {
        this.time_range = builder.time_range;
        this.limit = builder.limit;
    }

    public String endpoint() {
        return UriComponentsBuilder.fromPath("/me/top/tracks")
                .queryParam("time_range", time_range)
                .queryParam("limit", limit)
                .toUriString();
    }

    public List<Track> request(WebClient webClient, String accessToken) {
        return webClient.get()
                .uri(endpoint())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GetUsersTopTracksResponse.class)
                .map(response -> response.getItems().stream()
                        .map(item -> new Track(
                                item.getUri(),
                                item.getArtists().stream()
                                    .map(GetUsersTopTracksResponse.Artist::getName)
                                    .toList(),
                                item.getName(),
                                item.getAlbum().getImages().getFirst().getUrl()
                        ))
                        .collect(Collectors.toList())) // List<Track> 반환
                .block();
    }

    public static class Builder {

        // 선택 파라미터
        private String time_range = "medium_term";
        private int limit = 20;

        public Builder time_range(String time_range) {
            this.time_range = time_range;
            return this;
        }

        public Builder limit(int limit) {
//          assert (limit != null);
            assert (1 <= limit && limit <= 50);
            this.limit = limit;
            return this;
        }

        public GetUsersTopTracks build() {
            return new GetUsersTopTracks(this);
        }
    }
}
