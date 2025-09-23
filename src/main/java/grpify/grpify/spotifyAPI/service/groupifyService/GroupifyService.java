package grpify.grpify.spotifyAPI.service.groupifyService;


import grpify.grpify.auth.SpotifyTokenService;
import grpify.grpify.spotifyAPI.request.Search.SearchForItem;
import grpify.grpify.spotifyAPI.request.dto.Artist;
import grpify.grpify.spotifyAPI.request.dto.Track;
import groupifyApi.request.playlist.AddItemsToPlaylist;
import grpify.grpify.spotifyAPI.request.playlist.CreatePlaylist;
import grpify.grpify.spotifyAPI.request.users.GetUsersTopArtists;
import grpify.grpify.spotifyAPI.request.users.GetUsersTopTracks;
import grpify.grpify.spotifyAPI.service.OpenAiService;
import grpify.grpify.OAuthAccount.domain.OAuthAccount;
import grpify.grpify.OAuthAccount.repository.OAuthAccountRepository;
import grpify.grpify.spotifyAPI.request.Search.SearchForItem;
import grpify.grpify.spotifyAPI.request.dto.Track;
import grpify.grpify.spotifyAPI.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupifyService {

    private static final Logger logger = LoggerFactory.getLogger(GroupifyService.class);

    private final OAuthAccountRepository oAuthAccountRepository;
    private final WebClient webClient;
    private final OpenAiService openAiService;
    private final SpotifyTokenService spotifyTokenService;

    public List<Track> getUsersTopTracks(Long userId, String timeRange, int limit) {
        String accessToken = spotifyTokenService.getValidAccessToken(userId);

        GetUsersTopTracks request = new GetUsersTopTracks.Builder()
                .time_range(timeRange)
                .limit(limit)
                .build();

        return request.request(webClient, accessToken);
    }

    public List<Artist> getUsersTopArtists(Long userId, String timeRange, int limit) {
        String accessToken = spotifyTokenService.getValidAccessToken(userId);

        GetUsersTopArtists request = new GetUsersTopArtists.Builder()
                .time_range(timeRange)
                .limit(limit)
                .build();

        return request.request(webClient, accessToken);
    }

    public String createPlaylistAndAddTracks(Long userId, String name, String description, List<String> uris) {
        String accessToken = spotifyTokenService.getValidAccessToken(userId);

        String spotifyUserId = oAuthAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("OAuth account not found"))
                .getProviderId();

        String playlistId = new CreatePlaylist.Builder(spotifyUserId, name)
                .description(description)
                .public_(false)
                .build()
                .request(webClient, accessToken);

        new AddItemsToPlaylist.Builder(playlistId, uris)
                .position(null)
                .build()
                .request(webClient, accessToken);

        return playlistId;
    }

//    public User getUserProfile(Long userId) {
//        String accessToken = getValidAccessToken(userId);
//        return new GetCurrenttUsersProfile().request(webClient, accessToken);
//    }

    public List<Track> image2Playlist(Long userId, MultipartFile file) throws IOException {
        String accessToken = spotifyTokenService.getValidAccessToken(userId);

        List<String> qs = openAiService.processImage(file);
        List<Track> tracks = new ArrayList<>();

        for (String q : qs) {
            Track track = new SearchForItem.Builder(q).build().request(webClient, accessToken);
            tracks.add(track);
        }
        return tracks;
    }

    public Track search(Long userId, String q) {
        String accessToken = spotifyTokenService.getValidAccessToken(userId);

        return new SearchForItem.Builder(q)
                .build().request(webClient, accessToken);
    }
}
