package grpify.grpify.post.controller;

import grpify.grpify.auth.CustomUserDetails;
import grpify.grpify.common.dto.LikeResponseDto;
import grpify.grpify.post.dto.PostCreateRequest;
import grpify.grpify.post.dto.PostResponse;
import grpify.grpify.post.dto.PostSummaryResponse;
import grpify.grpify.post.dto.PostUpdateRequest;
import grpify.grpify.post.service.PostService;
import grpify.grpify.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    // 좋아요 요청 DTO를 record로 간결하게 정의
    public record LikeRequest(boolean like) {}

    /**
     * 게시글 생성 (Command)
     */
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User author = userDetails.getUser();
        Long newPostId = postService.write(request, author);

        // 생성 후, 최신 데이터를 다시 읽어서 클라이언트에게 반환
        PostResponse response = postService.read(newPostId, author.getId());

        URI location = URI.create("/api/posts/" + newPostId);
        return ResponseEntity.created(location).body(response);
    }

    /**
     * 게시글 단건 조회 (Query)
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> readPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = (userDetails != null) ? userDetails.getUser().getId() : null;
        postService.incrementViewCount(postId); // 조회수 증가
        PostResponse response = postService.read(postId, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시판별 게시글 목록 조회 (Query)
     */
    @GetMapping
    public ResponseEntity<Page<PostSummaryResponse>> findPostsByBoard(
            @RequestParam Long boardId,
            @PageableDefault(size = 10, sort = "createdAt,desc") Pageable pageable
    ) {
        Page<PostSummaryResponse> response = postService.findByBoard(boardId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 수정 (Command)
     */
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long authorId = userDetails.getUser().getId();
        postService.update(postId, authorId, request);

        // 수정 후, 최신 데이터를 다시 읽어서 반환
        PostResponse response = postService.read(postId, authorId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 삭제 (Command)
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long authorId = userDetails.getUser().getId();
        postService.softDelete(postId, authorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 좋아요 설정 (Command)
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeResponseDto> setLikeStatus(
            @PathVariable Long postId,
            @RequestBody LikeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser();
        LikeResponseDto response = postService.like(user, postId, request.like());
        return ResponseEntity.ok(response);
    }
}