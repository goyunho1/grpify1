package grpify.grpify.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentQueryDto {
    private Long commentId;
    private String content;
    private Long authorId;
    private String authorName;
    private String profileImgUrl;
    private int likeCount;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long parentCommentId;
    private int depth;
    private String parentAuthorName;
}

