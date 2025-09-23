package grpify.grpify.PostLike.domain;

import grpify.grpify.comment.domain.Comment;
import grpify.grpify.common.domain.BaseTimeEntity;
import grpify.grpify.post.domain.Post;
import grpify.grpify.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PostLike extends BaseTimeEntity {
    @Id
    @GeneratedValue
    @Column(name = "post_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}
