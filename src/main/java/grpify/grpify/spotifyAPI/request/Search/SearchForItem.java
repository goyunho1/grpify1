package grpify.grpify.spotifyAPI.request.Search;

import com.fasterxml.jackson.annotation.JsonProperty;
import grpify.grpify.spotifyAPI.request.dto.Track;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.List;

public class SearchForItem {
    private static final Logger logger = LoggerFactory.getLogger(SearchForItem.class);
    private final String q;
    private final String type;
    private final int limit;

    private SearchForItem(Builder builder) {
        this.q = builder.q;
        this.type = builder.type;
        this.limit = builder.limit;
    }

    public String endpoint() {
        return UriComponentsBuilder.fromPath("/search")
                .queryParam("q", q)
                .queryParam("type", type)
                .queryParam("limit", limit)
                .build(false)
                .toUriString();
    }

//    public String endpoint() {
//        String end = "/search?q=" + q + "&type=" + type + "&limit=" + limit + "&offset=0";
//        logger.info("🔹 endpoint: {}", end);
//        return end;
//    }

    public Track request(WebClient webClient, String accessToken) {
        Response response = webClient.get()
                .uri(endpoint())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Response.class)
                .block();

        if (response == null || response.getTracks() == null || response.getTracks().getItems() == null || response.getTracks().getItems().isEmpty()) {
            return new Track("N/A", List.of("Unknown Artist"), "Unknown Track", "Unknown Track");
        }

        Item item = response.getTracks().getItems().get(0);
        return new Track(
                item.getUri(),
                item.getArtists().stream().map(Artist::getName).toList(),
                item.getName(),
                item.getAlbum().getImages().get(2).getUrl()
        );
    }

    public static final class Builder {
        // 필수 파라미터
        private final String q;
        private final String type = "track";

        private final int limit = 10;

        public Builder(String q) {
            this.q = q;
        }

        public SearchForItem build() {
            return new SearchForItem(this);
        }
    }

    public static class Response {
        @Getter
        @JsonProperty("tracks")
        private Tracks tracks;
    }

    public static class Tracks {
        @Getter
        @JsonProperty("items")
        private List<Item> items;
    }

    @Getter
    public static class Item {
        @JsonProperty("uri")
        private String uri;

        @JsonProperty("name")
        private String name;

        @JsonProperty("artists")
        private List<Artist> artists;

        @JsonProperty("album")
        private Album album;
    }

    public static class Artist {
        @Getter
        @JsonProperty("name")
        private String name;
    }

    public static class Album {
        @Getter
        @JsonProperty("images")
        private List<Image> images;
    }

    public static class Image {
        @Getter
        @JsonProperty("url")
        private String url;
    }
}
