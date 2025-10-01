package grpify.grpify.comment.repository;

import grpify.grpify.comment.domain.Comment;

import grpify.grpify.comment.dto.CommentQueryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndIsDeletedFalse(Long id);



    /**
     * @param postId 댓글 목록 가져올 post
     * @return Page<CommentQueryDto> 인터페이스 기반 dto로 프로젝션,
     * alias 와 dto 필드명 일치 시켜주면 자동으로 생성해서 반환!
     * 서비스에서 pageable 객체 통해서 pagesize, offset 전달
     * UNION ALL 적용하는 SELECT 절 2개의 칼럼 정확히 일치하도록 주의
     * Page 객체 생성하려면 count 쿼리 결과값이 반드시 필요.
     * getTotalElements(), getTotalPages() 메서드에 필요함
     * native query 사용했기 때문에 count query 도 직접 작성해줘야 함<<< XXXX
     *
     * ->
     *
     * 댓글 작성 후에 해당 댓글로 화면 스크롤되도록 하려고 하는데 pageNumber 와 commentId 가 필요함.
     * ROW_NUMBER() OVER(ORDER BY sort_key ASC) 사용하면 되겠다 싶었는데 sort_key 를 임시테이블로 구했기 때문에 다시 구해야 됨.
     * 재귀 형태로 다시 sort_key 구해줘야하고 + 윈도우 함수 까지 사용하려니까 부담이 클 것 같음.
     * 인덱스 걸어줄 칼럼이 애매.
     * 댓글 작성 작업보다 읽기 작업이 더 빈번하다.
     * sort_key 를 서비스 레이어에서 댓글 작성시 생성하고 칼럼에도 추가, 인덱스 생성
     *
     * SELECT c FROM Comment c WHERE c.post.postId = :postId ORDER BY c.sortKey ASC 로 간단하게 변경...
     *
     * -> 읽기에서의 부담은 줄고 쓰기에서 늘어남.
     * -> 재귀, 함수 안써도 되기 때문에 native->jpql
     */

    @Query(
            value = """
                    WITH RECURSIVE comment_tree AS (
                        -- root 댓글: parent_comment_id IS NULL
                        SELECT
                            comment_id,
                            content,
                            likeCount,
                            is_deleted,
                            created_at,
                            updated_at,
                            parent_comment_id,
                            user_id,
                            post_id,
                            1 AS depth, -- root depth: 1
                            CAST(CONCAT(
                                DATE_FORMAT(created_at, '%Y%m%d%H%i%s%f'),
                                LPAD(comment_id, 10, '0')) AS CHAR(1000)) AS sort_key
                        FROM comment
                        WHERE post_id = :postId AND parent_comment_id IS NULL

                        UNION ALL

                        -- 자식 댓글 탐색, dfs 방식
                        SELECT
                            c.comment_id,
                            c.content,
                            c.like_count,
                            c.is_deleted,
                            c.created_at,
                            c.updated_at,
                            c.parent_comment_id,
                            c.user_id,
                            c.post_id,
                            ct.depth + 1,
                            CONCAT(
                                ct.sort_key,
                                '->',
                                DATE_FORMAT(c.created_at, '%Y%m%d%H%i%s%f'),
                                LPAD(c.comment_id, 10, '0'))    -- 결과: (00_0000_0001->00_0000_0002->...)
                        FROM comment c
                        INNER JOIN comment_tree ct ON c.parent_comment_id = ct.comment_id
                    )
                    SELECT
                        ct.comment_id AS commentId,
                        ct.content AS content,
                        u.id AS authorId,
                        u.username AS authorName,
                        u.profile_img_url AS profileImgUrl,
                        ct.like_count AS likeCount,
                        ct.is_deleted AS isDeleted,
                        ct.created_at AS createdAt,
                        ct.updated_at AS updatedAt,
                        ct.parent_comment_id AS parentCommentId,
                        ct.depth AS depth,
                        -- 부모 작성자명 추가 (@부모작성자명 + 자식 content 형식 ui 위해서)
                        parent_author.username AS parentAuthorName
                    FROM comment_tree ct
                    -- author 정보
                    LEFT JOIN user u ON ct.user_id = u.user_id
                    -- 부모 댓글(pc)과 부모 댓글 작성자(parent_author) 정보
                    LEFT JOIN comment pc ON ct.parent_comment_id = pc.comment_id
                    LEFT JOIN user parent_author ON pc.user_id = parent_author.user_id
                    ORDER BY ct.sort_key -- (01), (01->02), (01->02->03), (02), ... 순으로 정렬되도록!
                    """,
            countQuery = "SELECT COUNT(*) FROM comment c WHERE c.post_id = :postId",
            nativeQuery = true
    )
    Page<CommentQueryDto> findCommentHierarchyByPostId(
            @Param("postId") Long postId,
            Pageable pageable);

    // 아래 쿼리로 변경 <<<<<<
    @Query("""
            SELECT new grpify.grpify.comment.dto.CommentQueryDto(
                c.id,
                c.content,
                a.id,
                a.name,
                a.profileImgUrl,
                c.likeCount,
                c.isDeleted,
                c.createdAt,
                c.updatedAt,
                pc.id,
                c.depth,
                pa.name
            )
            FROM Comment c
            LEFT JOIN c.author a
            LEFT JOIN c.parentComment pc
            LEFT JOIN pc.author pa
            WHERE c.post.id = :postId
            ORDER BY c.sortKey
            """
    )//left join 사용 이유?
    Page<CommentQueryDto> findCommentsByPostId(
            @Param("postId") Long postId,
            Pageable pageable
    );



    /**
     *    CREATE INDEX idx_comment_rank_calculation
     *    ON comment (post_id, sort_key)
     *    COMMENT '댓글 순위 계산용 인덱스';
     *    (post_id, sort_key) 로 인덱스 설정
     *
     *    1. post_id 값 찾아 빠르게 이동
     *    2. sort_key 기준 range scan
     *    3. 인덱스만으로 COUNT 완료 (커버링 인덱스)
     *
     *    TODO:
     *    인덱스 키의 크기가 클수록 하나의 인덱스 페이지에 저장할 수 있는 키의 수가 적어지는 것으로 알고있는데 현재의 sort_key 가 너무 크지는 않을까?
     *    비교해보고 성능차이 크다면 sortKey 길이 제한, depth 제한 필요할 듯...
     *    UI 문제로 어차피 depth 제한 해야함. -> ui 제작 해보고 가능한 depth에 따라 조정하면 될 듯??
     *
     */
    long countByPost_IdAndSortKeyLessThanEqual(Long postId, String sortKey);



    /**
     * @param postId
     * _@Modifying @Query를 통해 update, insert 쿼리 실행 시,
     *  em.executeUpdate() 실행 -> 벌크 업데이트
     *  clearAutomatically = true -> 벌크 업데이트 전, 영속성 컨텍스트 초기화
     *  jpql, native 차이점 유의하며 작성 (isDeleted - is_deleted)
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE Comment c
            SET c.isDeleted = true
            WHERE c.post.id = :postId
            """)
    void bulkSoftDeleteByPostId(@Param("postId") Long postId);



    //1
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE Comment c
            SET c.isDeleted = true
            WHERE c.post.board.id = :boardId
            """)
    void bulkSoftDeleteByBoardIdJpql(@Param("boardId") Long boardId);

    //2
    @Query(value = """
            UPDATE comment c
            INNER JOIN post p ON c.post_id = p.post_id
            SET c.is_deleted = 1, c.updated_at = NOW()
            WHERE p.board_id = :boardId
            AND c.is_deleted = 0
            """, nativeQuery = true)
    void bulkSoftDeleteByBoardIdNative(@Param("boardId") Long boardId);

    //+batch
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE comment c
            INNER JOIN post p ON c.post_id = p.post_id
            SET c.is_deleted = 1, c.updated_at = NOW()
            WHERE p.board_id = :boardId
            AND c.is_deleted = 0
            LIMIT :batchSize
            """, nativeQuery = true)
    int bulkSoftDeleteByBoardIdBatch(@Param("boardId") Long boardId,
                                     @Param("batchSize") int batchSize);
}


