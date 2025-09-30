package grpify.grpify.comment.controller;

import grpify.grpify.auth.CustomUserDetails;
import grpify.grpify.comment.dto.*;
import grpify.grpify.comment.service.CommentService;
import grpify.grpify.commentLike.dto.LikeResponse;
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
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 목록 조회 (게시글별)
     * 모든 사용자가 접근 가능 (비로그인 포함)
     * 계층구조로 정렬되어 반환
     */
    @GetMapping
    public ResponseEntity<Page<CommentsResponse>> getComments(
            @RequestParam Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "sortKey") Pageable pageable) {
        
        Long currentUserId = (userDetails != null) ? userDetails.getUser().getId() : null;
        Page<CommentsResponse> comments = commentService.findCommentsByPost(postId, currentUserId, pageable);
        return ResponseEntity.ok(comments);
    }

//    /**
//     * 댓글 조회 (단일)
//     * GET /api/comments/{commentId}
//     */
//    @GetMapping("/comments/{commentId}")
//    public ResponseEntity<CommentsResponse> getComment(
//            @PathVariable Long commentId,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        Long currentUserId = (userDetails != null) ? userDetails.getUser().getId() : null;
//        CommentsResponse comment = commentService.findById(commentId, currentUserId);
//        return ResponseEntity.ok(comment);
//    }

    /**
     * 댓글 작성 (일반 댓글 및 대댓글)
     * 로그인된 사용자만 접근 가능
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentWriteResponse> createComment(
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long authorId = userDetails.getUser().getId();
        CommentWriteResponse response = commentService.write(request, authorId);
        
        URI location = URI.create("/api/comments/" + response.getCommentId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * 댓글 수정
     * 로그인된 사용자 중 작성자만 접근 가능 (권한 확인은 서비스에서)
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentWriteResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long authorId = userDetails.getUser().getId();
        CommentWriteResponse response = commentService.update(request, commentId, authorId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     * 로그인된 사용자 중 작성자만 접근 가능 (권한 확인은 서비스에서)
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getUser().getId();
        commentService.softDelete(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 댓글 좋아요/취소
     * 로그인된 사용자만 접근 가능
     */
    @PostMapping("/{commentId}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> toggleCommentLike(
            @PathVariable Long commentId,
            @Valid @RequestBody LikeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getUser().getId();
        LikeResponse response = commentService.like(userId, commentId, request.isLike()); //boolean 타입은 getXX 대신 isXX 로 생성.
        return ResponseEntity.ok(response);
    }
}