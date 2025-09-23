package grpify.grpify.post.repository;

import grpify.grpify.board.domain.Board;
import grpify.grpify.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    //id, name 조회 방식 비교 해보기
    //예측 : id 가 pk 이므로 인덱스가 존재해서 빠름
    //name 으로 조회 했을 때 관리가 편함. board_id = 1 보다 board_name = "자유게시판" 이 직관적,
    //board 는 개수가 많지 않아 속도 차이가 무의미

    // 목록 조회
//    Page<Post> findByBoardNameAndIsDeletedFalseOrderByCreatedAtDesc(String boardName, Pageable pageable);
//    Page<Post> findByBoardIdIsDeletedFalseOrderByCreatedAtDesc(Long boardId, Pageable pageable);
//    이렇게 메서드 명명 시에 정렬 조건을 붙이며 하드코딩하는건 재사용성이 너무 부족하다.
//    추후에 추천순, 조회순 정렬이 필요하다면 새로 작성해야 함;;
//      => 컨트롤러에서 pageable 을 통해서 정렬 파라미터를 받아오는 방식을 사용한다면 재사용 가능!


    Page<Post> findByBoardAndDeletedFalse(Board board, Pageable pageable);
    // 게시글 삭제 목적 조회는 페이징 필요x

    Optional<Post> findByIdAndIsDeletedFalse(Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE Post p
            SET p.isDeleted = true
            WHERE p.board.id = :boardId
            """)
    void bulkSoftDeleteByBoardId(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);
}

