package grpify.grpify.commentLike.repository;

import grpify.grpify.comment.domain.Comment;
import grpify.grpify.commentLike.domain.CommentLike;
import grpify.grpify.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    // index (userId, commentId) 순서
    Optional<CommentLike> findByUserAndComment(User user, Comment comment);

    @Query(value = """
            SELECT cl.comment.id
            FROM CommentLike cl
            WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds
            """)
    Set<Long> findLikedComments( // Set.contains 가 List.contains 보다 이득
                                 @Param("userId") Long userId,
                                 @Param("commentIds") List<Long> commentIds);
}
