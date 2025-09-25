package grpify.grpify.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CommentRequest {

    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long postId;
    
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 1000, message = "댓글은 1000자를 초과할 수 없습니다.")
    private String content;
    
    private Long parentCommentId; // 대댓글인 경우만 필요
}
