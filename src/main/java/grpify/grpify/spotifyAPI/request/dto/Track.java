package grpify.grpify.spotifyAPI.request.dto;

import java.util.List;

public record Track(String uri, List<String> artists, String title, String cover) { }
