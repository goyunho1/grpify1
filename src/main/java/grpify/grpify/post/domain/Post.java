package grpify.grpify.post.domain;

import grpify.grpify.PostLike.domain.PostLike;
import grpify.grpify.board.domain.Board;
import grpify.grpify.comment.domain.Comment;
import grpify.grpify.common.domain.BaseTimeEntity;
import grpify.grpify.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Post extends BaseTimeEntity {
    @Id @GeneratedValue
    @Column(name = "post_id")
    private Long id;

    @Column(nullable = false, length = 30)
    private String title;

    private String content;

    @Column(nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    // @Version//for test - 성능 테스트를 위해 임시 제거
    // private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

//    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
//    @Builder.Default
//    private List<Comment> comments = new ArrayList<>();
//    댓글 페이징 사용하기 때문에 제거


//    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
//    @Builder.Default
//    private List<Attachment> attachments = new ArrayList<>();


    // 게시글 내용 수정 (타이틀, 본문)
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    // 조회수 증가
    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    // 댓글 수 감소
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    // 좋아요 수 증가/감소 (PostLike 엔티티와 동기화)
    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

}
