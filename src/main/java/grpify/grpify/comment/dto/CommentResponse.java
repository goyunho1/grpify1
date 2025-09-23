package grpify.grpify.comment.dto;

import grpify.grpify.comment.domain.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {
    private Long commentId;
    private String content;
    private int likeCount;
    private Long authorId;
    private String authorProfileImgUrl;
    private Long postId;
    private Long parentCommentId;

    private String parentAuthorName;

    private int depth;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .authorId(comment.getAuthor().getId())
                .authorProfileImgUrl(comment.getAuthor().getProfileImgUrl())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getId())
                .parentAuthorName(comment.getParentComment().getAuthor().getName())
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
