package grpify.grpify.PostLike.repository;

import grpify.grpify.PostLike.domain.PostLike;
import grpify.grpify.comment.domain.Comment;
import grpify.grpify.commentLike.domain.CommentLike;
import grpify.grpify.post.domain.Post;
import grpify.grpify.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUserAndPost(User user, Post post);

    @Query(value = """
            SELECT cl.comment.id
            FROM CommentLike cl
            WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds
            """)
    Set<Long> findLikedComments( // Set.contains 가 List.contains 보다 이득
                                 @Param("userId") Long userId,
                                 @Param("commentIds") List<Long> commentIds);
    Page<PostLike> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUser_IdAndPost(Long userId, Post post);
}
