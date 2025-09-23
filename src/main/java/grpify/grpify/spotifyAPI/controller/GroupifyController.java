package grpify.grpify.spotifyAPI.controller;

import com.nimbusds.jwt.JWT;
import grpify.grpify.auth.CustomUserDetails;
import grpify.grpify.spotifyAPI.request.dto.Artist;
import grpify.grpify.spotifyAPI.request.dto.Track;
import grpify.grpify.spotifyAPI.service.groupifyService.GroupifyService;
import grpify.grpify.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @AuthenticationPrincipal :
 * 현재 요청을 처리 중인 스레드의 보안 컨텍스트(Security Context)에 저장된 인증(Authentication) 객체에서,
 * Principal(사용자 주체) 객체를 꺼내서 이 파라미터에 넣어주세요." 라는 지시어
 *
 * //JwtAuthenticationFilter.java :
 *             // DB 에서 user 조회한 후 UserDetails 로 감싸줌
 *             UserDetails userDetails = customUserDetailsService.loadUserByUsername(userIdStr); <<< Principal
 *
 *             // Authentication 객체 생성
 *             UsernamePasswordAuthenticationToken authenticationToken =
 *                     new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
 *
 *             // SecurityContext 에 등록
 *             SecurityContextHolder.getContext().setAuthentication(authenticationToken);
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/groupify")
public class GroupifyController {

    private final GroupifyService groupifyService;


    @GetMapping("/top-tracks")
    public List<Track> usersTopTracks2Playlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "medium_term") String time_range,
            @RequestParam(defaultValue = "10") int limit) {

        return groupifyService.getUsersTopTracks(userDetails.getUser().getId(), time_range, limit);
    }

    @GetMapping("/top-artists")
    public List<Artist> usersTopArtists(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "medium_term") String time_range,
            @RequestParam(defaultValue = "10") int limit) {

        return groupifyService.getUsersTopArtists(userDetails.getUser().getId(), time_range, limit);
    }

    @PostMapping("/create-playlist")
    public String createPlaylist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PlaylistRequest request) {

        return groupifyService.createPlaylistAndAddTracks(
                userDetails.getUser().getId(),
                request.name(),
                request.description(),
                request.uris()
        );
    }
    public record PlaylistRequest(String name, String description, List<String> uris) {}


    @GetMapping("/search")
    public Track searchTrack(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String q) {

        return groupifyService.search(userDetails.getUser().getId(), q);
    }

//    @GetMapping("/user-profile")
//    public User usersProfile(@AuthenticationPrincipal User user) {
//        return groupifyService.getUserProfile(user.getId());
//    }

    @GetMapping("/user-profile")
    public UserProfileDto  userProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return new UserProfileDto(user.getProfileImgUrl(), user.getName());
    }
    public record UserProfileDto(String image, String display_name) {}


}

//CustomOAuth2User (implements OAuth2User):
//역할: OAuth2 로그인 흐름에서 Spring Security가 요구하는 Principal의 표준. Spotify/Google로부터 받은 원본 사용자 속성(attributes)을 담고, 우리 User 엔터티와 연결하는 브릿지 역할을 합니다.
//사용처: CustomOAuth2UserService에서 생성되어 OAuth2AuthenticationSuccessHandler로 전달됩니다.
//CustomUserDetails (implements UserDetails):
//역할: JWT 인증 흐름에서 Spring Security가 요구하는 Principal의 표준. 우리 User 엔터티를 기반으로 Spring Security의 인가(Authorization) 시스템이 필요로 하는 권한(authorities)과 계정 상태 정보를 제공합니다.
//        사용처: JwtAuthenticationFilter에서 생성되어 SecurityContext에 저장되고, 컨트롤러의 @AuthenticationPrincipal로 주입됩니다.
//이처럼 두 클래스는 각자의 책임 영역이 명확하게 분리되어 있습니다.