package grpify.grpify.post.service;

import grpify.grpify.board.domain.Board;
import grpify.grpify.board.repository.BoardRepository;
import grpify.grpify.post.domain.Post;
import grpify.grpify.post.repository.PostRepository;
import grpify.grpify.user.domain.User;
import grpify.grpify.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PostViewCountSimpleTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    private Post testPost;
    private User testUser;
    private Board testBoard;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testUser = User.builder()
                .name("testuser")
                .email("test@example.com")
                .build();
        userRepository.save(testUser);

        testBoard = Board.builder()
                .name("테스트게시판")
                .description("테스트용 게시판")
                .build();
        boardRepository.save(testBoard);

        testPost = Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .author(testUser)
                .board(testBoard)
                .viewCount(0)
                .build();
        postRepository.save(testPost);
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        userRepository.deleteAll();
        boardRepository.deleteAll();
    }

    @Test
    @DisplayName("단순 조회수 증가 테스트 - 엔티티 방식")
    void testEntityApproach() {
        // given
        int initialViewCount = testPost.getViewCount();

        // when
        postService.incrementViewCountEntity(testPost.getId());

        // then
        Post updatedPost = postRepository.findById(testPost.getId()).get();
        assertThat(updatedPost.getViewCount()).isEqualTo(initialViewCount + 1);
    }

    @Test
    @DisplayName("단순 조회수 증가 테스트 - 벌크 업데이트 방식")
    void testBulkUpdateApproach() {
        // given
        int initialViewCount = testPost.getViewCount();

        // when
        postService.incrementViewCount(testPost.getId());

        // then
        Post updatedPost = postRepository.findById(testPost.getId()).get();
        assertThat(updatedPost.getViewCount()).isEqualTo(initialViewCount + 1);
    }

    @Test
    @DisplayName("단순 조회수 증가 테스트 - 비관적 락 방식")
    void testPessimisticLockApproach() {
        // given
        int initialViewCount = testPost.getViewCount();

        // when
        postService.incrementViewCountPessimistic(testPost.getId());

        // then
        Post updatedPost = postRepository.findById(testPost.getId()).get();
        assertThat(updatedPost.getViewCount()).isEqualTo(initialViewCount + 1);
    }

    @Test
    @DisplayName("단순 조회수 증가 테스트 - 낙관적 락 방식")
    void testOptimisticLockApproach() {
        // given
        int initialViewCount = testPost.getViewCount();

        // when
        postService.incrementViewCountOptimistic(testPost.getId());

        // then
        Post updatedPost = postRepository.findById(testPost.getId()).get();
        assertThat(updatedPost.getViewCount()).isEqualTo(initialViewCount + 1);
    }

    @Test
    @DisplayName("연속 조회수 증가 테스트 - 각 방식별 10회 연속 실행")
    void testConsecutiveIncrements() {
        // given
        int incrementCount = 10;

        // Case 1: 엔티티 방식
        Post entityPost = createTestPost("엔티티방식");
        for (int i = 0; i < incrementCount; i++) {
            postService.incrementViewCountEntity(entityPost.getId());
        }
        assertThat(postRepository.findById(entityPost.getId()).get().getViewCount())
                .isEqualTo(incrementCount);

        // Case 2: 벌크 업데이트 방식
        Post bulkPost = createTestPost("벌크업데이트");
        for (int i = 0; i < incrementCount; i++) {
            postService.incrementViewCount(bulkPost.getId());
        }
        assertThat(postRepository.findById(bulkPost.getId()).get().getViewCount())
                .isEqualTo(incrementCount);

        // Case 3: 비관적 락 방식
        Post pessimisticPost = createTestPost("비관적락");
        for (int i = 0; i < incrementCount; i++) {
            postService.incrementViewCountPessimistic(pessimisticPost.getId());
        }
        assertThat(postRepository.findById(pessimisticPost.getId()).get().getViewCount())
                .isEqualTo(incrementCount);

        // Case 4: 낙관적 락 방식
        Post optimisticPost = createTestPost("낙관적락");
        for (int i = 0; i < incrementCount; i++) {
            postService.incrementViewCountOptimistic(optimisticPost.getId());
        }
        assertThat(postRepository.findById(optimisticPost.getId()).get().getViewCount())
                .isEqualTo(incrementCount);
    }

    private Post createTestPost(String title) {
        Post post = Post.builder()
                .title(title)
                .content("테스트 내용")
                .author(testUser)
                .board(testBoard)
                .viewCount(0)
                .build();
        return postRepository.save(post);
    }
}
