package grpify.grpify.spotifyAPI.controller;

import grpify.grpify.auth.CustomUserDetails;
import grpify.grpify.spotifyAPI.request.dto.Track;
import grpify.grpify.spotifyAPI.service.groupifyService.GroupifyService;
import grpify.grpify.user.domain.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/openai")
public class OpenAiController {

    private final GroupifyService groupifyService;

    public OpenAiController(GroupifyService groupifyService) {
        this.groupifyService = groupifyService;
    }

    @PostMapping("/process-image")
    public List<Track> processImage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @RequestParam("file") MultipartFile file) throws IOException {
        return groupifyService.image2Playlist(userDetails.getUser().getId(), file);
    }
}
