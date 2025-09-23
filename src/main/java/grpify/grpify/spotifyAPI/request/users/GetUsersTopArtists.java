package grpify.grpify.spotifyAPI.request.users;

import com.fasterxml.jackson.annotation.JsonProperty;

import grpify.grpify.spotifyAPI.request.dto.Artist;
import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class GetUsersTopArtists {

    private final String time_range;
    private final int limit;

    private GetUsersTopArtists(Builder builder) {
        this.time_range = builder.time_range;
        this.limit = builder.limit;
    }

    public String endpoint() {
        return UriComponentsBuilder.fromPath("/me/top/artists")
                .queryParam("time_range", time_range)
                .queryParam("limit", limit)
                .toUriString();
    }

    public List<Artist> request(WebClient webClient, String accessToken) { // WebClient 주입 받는 방식 고려.
        return webClient.get()
                .uri(endpoint())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Response.class)
                .map(response -> response.getItems().stream()
                        .map(item -> new Artist(
                                item.getName(),
                                item.getImages().getFirst().getUrl(),
                                item.getExternal_urls().getUrl(),
                                item.getGenres()
                        ))
                        .collect(Collectors.toList())) // List<Artist> 반환
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

        public GetUsersTopArtists build() {
            return new GetUsersTopArtists(this);
        }
    }

    public static class Response {
        @Getter
        @JsonProperty("items")
        private List<Item> items;
    }

    @Getter
    public static class Item {
        @JsonProperty("name")
        private String name;

        @JsonProperty("images")
        private List<Image> images;

        @JsonProperty("external_urls")
        private ArtistUrl external_urls;

        @JsonProperty("genres")
        private List<String> genres;
    }

    public static class Image {
        @Getter
        @JsonProperty("url")
        private String url;
    }

    public static class ArtistUrl {
        @Getter
        @JsonProperty("spotify")
        private String url;
    }



}