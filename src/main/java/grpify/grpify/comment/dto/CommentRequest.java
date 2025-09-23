package grpify.grpify.comment.dto;

import lombok.Getter;

@Getter

public class CommentRequest {

    private Long postId; //pathvariable
    private String content;
    private Long parentCommentId; //nullable
}
