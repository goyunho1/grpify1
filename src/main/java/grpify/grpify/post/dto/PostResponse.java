package grpify.grpify.post.dto;

import grpify.grpify.post.domain.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
@Getter
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private int viewCount;
    private int likeCount;
    private String authorName;
    private Long authorId;
    private String boardName;
    private Long boardId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isLiked;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .authorName(post.getAuthor().getName())
                .authorId(post.getAuthor().getId())
                .boardName(post.getBoard().getName())
                .boardId(post.getBoard().getId())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public static PostResponse from(Post post, boolean isLiked) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .authorName(post.getAuthor().getName())
                .authorId(post.getAuthor().getId())
                .boardName(post.getBoard().getName())
                .boardId(post.getBoard().getId())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .isLiked(isLiked) // <<<<<<<<<<<<< 추가
                .build();
    }
}
