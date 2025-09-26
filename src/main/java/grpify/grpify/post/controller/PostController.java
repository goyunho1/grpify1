package grpify.grpify.post.controller;

import grpify.grpify.auth.CustomUserDetails;
import grpify.grpify.PostLike.dto.LikeResponse;
import grpify.grpify.board.service.BoardService;
import grpify.grpify.post.dto.PostRequest;
import grpify.grpify.post.dto.PostResponse;
import grpify.grpify.post.service.PostService;
import grpify.grpify.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/{boardId}/posts")
public class PostController {

    private final PostService postService;
    private final BoardService boardService;

    // 좋아요 요청 DTO를 record로 간결하게 정의
    public record LikeRequest(boolean like) {}

    /**
     * 게시글 목록 조회 (게시판별)
     * 모든 사용자가 접근 가능 (비로그인 포함)
     */
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPostsByBoard(
            @PathVariable Long boardId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

//        boardService.findById(boardId); //postService.findByBoard 에서 검사

        Page<PostResponse> posts = postService.findByBoard(boardId, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 상세 조회
     * 모든 사용자가 접근 가능 (비로그인 포함)
     * 조회수 자동 증가
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long currentUserId = (userDetails != null) ? userDetails.getUser().getId() : null;

        // 게시글 조회 (좋아요 여부 포함)
        PostResponse post = postService.read(postId, currentUserId);
        // 조회수 증가 (별도 트랜잭션)
        postService.incrementViewCount(postId);

        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 작성
     * 로그인된 사용자만 접근 가능
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> createPost(
            @PathVariable Long boardId,
            @Valid @RequestBody PostRequest request, // boardId 제외된 DTO
            @AuthenticationPrincipal CustomUserDetails userDetails) {

//        boardService.findById(boardId);

        User author = userDetails.getUser();
        Long newPostId = postService.write(request, boardId, author);

        PostResponse post = postService.read(newPostId, author.getId());
        URI location = URI.create("/api/boards/" + boardId + "/posts/" + newPostId);

        return ResponseEntity.created(location).body(post);
    }

    /**
     * 게시글 수정
     * 로그인된 사용자 중 작성자만 접근 가능 (권한 확인은 서비스에서)
     */
    @PutMapping("/{postId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // PostRequest에 ID 설정
        request.setId(postId);
        postService.update(request);
        
        return ResponseEntity.ok().build();
    }

    /**
     * 게시글 삭제 (소프트 삭제)
     * 로그인된 사용자 중 작성자만 접근 가능 (권한 확인은 서비스에서)
     */
    @DeleteMapping("/{postId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        postService.softDelete(postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 좋아요/취소
     * 로그인된 사용자만 접근 가능
     */
    @PostMapping("/{postId}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> toggleLike(
            @PathVariable Long postId,
            @Valid @RequestBody LikeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser();
        LikeResponse response = postService.like(user.getId(), postId, request.like());
        return ResponseEntity.ok(response);
    }
}