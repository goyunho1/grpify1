package grpify.grpify.comment.controller;

import grpify.grpify.comment.dto.CommentRequest;
import grpify.grpify.comment.dto.CommentResponse;
import grpify.grpify.comment.dto.CommentsResponse;
import grpify.grpify.comment.dto.LikeRequest;
import grpify.grpify.comment.service.CommentService;
import grpify.grpify.commentLike.dto.LikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor

public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<Page<CommentsResponse>> getComment(
            @RequestParam Long postId,
            @RequestParam Long userId,
            Pageable pageable) {

        Page<CommentsResponse> response = commentService.findCommentsByPost(postId, userId, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<LikeResponse> likeComment(
            @PathVariable Long commentId,
            @RequestBody LikeRequest request) {

        Long userId = 1L;
        LikeResponse response = commentService.like(commentId, userId, request.isLike());
        return ResponseEntity.ok(response);
    }

}
