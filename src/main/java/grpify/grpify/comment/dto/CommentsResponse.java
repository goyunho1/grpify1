package grpify.grpify.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 생성 메서드 (from)에 isDeleted 체크 로직 포함.
 */
@Getter
@Builder
public class CommentsResponse {
    private final Long commentId;
    private final String content;
    private final Long authorId;
    private final String authorName;
    private final String profileImgUrl;
    private final int likeCount;
    // 응답에 포함시킬 필요 x
    //private final boolean isDeleted;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long parentCommentId;
    private final String parentAuthorName;
    private final int depth;
    private final boolean isLiked;



    public static CommentsResponse from(CommentQueryDto dto, boolean isLiked) {

        CommentsResponseBuilder builder = CommentsResponse.builder()
                .commentId(dto.getCommentId())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .parentCommentId(dto.getParentCommentId())
                .parentAuthorName(dto.getParentAuthorName())
                .depth(dto.getDepth())
                .isLiked(isLiked); // 따로 받아왔음


        // isDeleted 체크
        if (dto.getIsDeleted()) {
            builder.content("삭제된 댓글입니다.")
                    .authorId(null)
                    .authorName(null)
                    .profileImgUrl(null)
                    .likeCount(0);
        }
        else {
            builder.content(dto.getContent())
                    .authorId(dto.getAuthorId())
                    .authorName(dto.getAuthorName())
                    .profileImgUrl(dto.getProfileImgUrl());
        }

        return builder.build();
    }
}
