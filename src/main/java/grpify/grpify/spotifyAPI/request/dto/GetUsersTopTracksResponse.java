package grpify.grpify.spotifyAPI.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class GetUsersTopTracksResponse {

    @JsonProperty("items")
    private List<Item> items;

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

    @Getter
    public static class Artist {

        @JsonProperty("name")
        private String name;
    }

    @Getter
    public static class Album {

        @JsonProperty("images")
        private List<Image> images;
    }

    @Getter
    public static class Image {

        @JsonProperty("url")
        private String url;
    }
}