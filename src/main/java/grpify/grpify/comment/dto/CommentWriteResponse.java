package grpify.grpify.comment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentWriteResponse {
    private int pageNumber;
    private Long commentId;

    public static CommentWriteResponse from(int pageNumber, Long commentId) {
        return CommentWriteResponse.builder()
                .pageNumber(pageNumber)
                .commentId(commentId)
                .build();
    }
}
