package grpify.grpify.spotifyAPI.request.dto;

import java.util.List;

public record Artist(String name, String image, String url, List<String> genres) {
}
