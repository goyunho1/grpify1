package grpify.grpify.post;

import grpify.grpify.board.domain.Board;
import grpify.grpify.board.repository.BoardRepository;
import grpify.grpify.post.domain.Post;
import grpify.grpify.post.repository.PostRepository;
import grpify.grpify.post.service.PostService;
import grpify.grpify.user.domain.User;
import grpify.grpify.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("test")
class PostViewCountPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(PostViewCountPerformanceTest.class);

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    private User testUser;
    private Board testBoard;

    @BeforeEach
    void setUp() {
        // 테스트 시작 전 데이터 정리
        postRepository.deleteAll();
        userRepository.deleteAll();
        boardRepository.deleteAll();
        
        // 매번 다른 username으로 생성 (unique 제약조건 때문)
        testUser = User.builder()
                .name("testuser_" + System.currentTimeMillis())
                .email("test_" + System.currentTimeMillis() + "@example.com")
                .build();
        testUser = userRepository.save(testUser); // 저장 후 ID 할당된 객체 다시 받기
        System.out.println("Saved User ID: " + testUser.getId()); // 디버깅용

        testBoard = Board.builder()
                .name("테스트게시판_" + System.currentTimeMillis())
                .description("테스트용 게시판")
                .build();
        testBoard = boardRepository.save(testBoard); // 저장 후 ID 할당된 객체 다시 받기
        System.out.println("Saved Board ID: " + testBoard.getId()); // 디버깅용
    }

    @AfterEach
    void tearDown() {
        // 테스트 완료 후에는 데이터를 유지 (삭제하지 않음)
        // 데이터는 다음 테스트 시작 전 @BeforeEach에서 삭제됨
    }

    @Test
    @DisplayName("조회수 증가 처리 방식별 성능 비교 테스트")
    void compareViewCountIncrementMethods() throws InterruptedException {
        // 테스트 설정
        int threadCount = 100;
        int totalRequests = 1000;
        
        log.info("=== 조회수 증가 처리 방식별 성능 비교 ===");
        log.info("스레드 수: {}", threadCount);
        log.info("총 요청 수: {}", totalRequests);
        log.info("");

        try {
            // Case 1: 엔티티 필드값 수정 방식 (동시성 오류 발생)
            testCase1EntityApproach(threadCount, totalRequests);
        } catch (Exception e) {
            log.error("Case 1 테스트 중 예외 발생: {}", e.getMessage(), e);
        }
        
        try {
            // Case 2: 벌크 업데이트 방식
            testCase2BulkUpdateApproach(threadCount, totalRequests);
        } catch (Exception e) {
            log.error("Case 2 테스트 중 예외 발생: {}", e.getMessage(), e);
        }
        
        try {
            // Case 3: 비관적 락 방식
            testCase3PessimisticLockApproach(threadCount, totalRequests);
        } catch (Exception e) {
            log.error("Case 3 테스트 중 예외 발생: {}", e.getMessage(), e);
        }
        
//        try {
//            // Case 4: 낙관적 락 방식
//            testCase4OptimisticLockApproach(threadCount, totalRequests);
//        } catch (Exception e) {
//            log.error("Case 4 테스트 중 예외 발생: {}", e.getMessage(), e);
//        }
    }

    private void testCase1EntityApproach(int threadCount, int totalRequests) throws InterruptedException {
        log.info(">>> Case 1 시작: 엔티티 방식 테스트");
        Post testPost = createTestPost("Case1_엔티티방식");
        
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    postService.incrementViewCountEntity(testPost.getId());
                } catch (Exception e) {
                    log.error("Case 1 - 엔티티 방식에서 오류 발생: {}", e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        Post result = postRepository.findById(testPost.getId()).get();
        
        // TPS 계산
        double tps = (result.getViewCount() * 1000.0) / executionTime;
        
        log.info("Case 1: 엔티티 필드값 수정 방식");
        log.info("  처리 시간: {}ms", executionTime);
        log.info("  평균 응답 시간: {}ms", String.format("%.2f", executionTime / (double) totalRequests));
        log.info("  TPS (초당 처리량): {}", String.format("%.1f", tps));
        log.info("  최종 조회수: {}/{}", result.getViewCount(), totalRequests);
        log.info("  정확성: {}%", String.format("%.1f", result.getViewCount() * 100.0 / totalRequests));
        log.info("");
    }

    private void testCase2BulkUpdateApproach(int threadCount, int totalRequests) throws InterruptedException {
        log.info(">>> Case 2 시작: 벌크 업데이트 방식 테스트");
        Post testPost = createTestPost("Case2_벌크업데이트");
        
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    postService.incrementViewCount(testPost.getId());
                } catch (Exception e) {
                    log.error("Case 2 - 벌크 업데이트에서 오류 발생: {}", e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        Post result = postRepository.findById(testPost.getId()).get();
        
        // TPS 계산
        double tps = (result.getViewCount() * 1000.0) / executionTime;
        
        log.info("Case 2: 벌크 업데이트 방식");
        log.info("  처리 시간: {}ms", executionTime);
        log.info("  평균 응답 시간: {}ms", String.format("%.2f", executionTime / (double) totalRequests));
        log.info("  TPS (초당 처리량): {}", String.format("%.1f", tps));
        log.info("  최종 조회수: {}/{}", result.getViewCount(), totalRequests);
        log.info("  정확성: {}%", String.format("%.1f", result.getViewCount() * 100.0 / totalRequests));
        log.info("");
    }

    private void testCase3PessimisticLockApproach(int threadCount, int totalRequests) throws InterruptedException {
        log.info(">>> Case 3 시작: 비관적 락 방식 테스트");
        Post testPost = createTestPost("Case3_비관적락");
        
        // 비관적 락은 너무 느리므로 요청 수를 줄임
        int reducedRequests = totalRequests / 10; // 100개로 줄임
        
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(reducedRequests);
        
        for (int i = 0; i < reducedRequests; i++) {
            executor.submit(() -> {
                try {
                    postService.incrementViewCountPessimistic(testPost.getId());
                } catch (Exception e) {
                    log.error("Case 3 - 비관적 락에서 오류 발생: {}", e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS); // 타임아웃 증가
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        Post result = postRepository.findById(testPost.getId()).get();
        
        // TPS 계산
        double tps = (result.getViewCount() * 1000.0) / executionTime;
        
        log.info("Case 3: 비관적 락 방식 (요청 수 {}개로 축소)", reducedRequests);
        log.info("  처리 시간: {}ms", executionTime);
        log.info("  평균 응답 시간: {}ms", String.format("%.2f", executionTime / (double) reducedRequests));
        log.info("  TPS (초당 처리량): {}", String.format("%.1f", tps));
        log.info("  최종 조회수: {}/{}", result.getViewCount(), reducedRequests);
        log.info("  정확성: {}%", String.format("%.1f", result.getViewCount() * 100.0 / reducedRequests));
        log.info("");
    }

    private void testCase4OptimisticLockApproach(int threadCount, int totalRequests) throws InterruptedException {
        log.info(">>> Case 4 시작: 낙관적 락 방식 테스트");
        Post testPost = createTestPost("Case4_낙관적락");
        
        // 낙관적 락도 재시도로 인해 느리므로 요청 수를 줄임
        int reducedRequests = totalRequests / 5; // 200개로 줄임
        
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(reducedRequests);
        
        for (int i = 0; i < reducedRequests; i++) {
            executor.submit(() -> {
                try {
                    postService.incrementViewCountOptimistic(testPost.getId());
                } catch (Exception e) {
                    log.error("Case 4 - 낙관적 락에서 오류 발생: {}", e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS); // 타임아웃 증가
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        Post result = postRepository.findById(testPost.getId()).get();
        
        // TPS 계산
        double tps = (result.getViewCount() * 1000.0) / executionTime;
        
        log.info("Case 4: 낙관적 락 방식 (요청 수 {}개로 축소)", reducedRequests);
        log.info("  처리 시간: {}ms", executionTime);
        log.info("  평균 응답 시간: {}ms", String.format("%.2f", executionTime / (double) reducedRequests));
        log.info("  TPS (초당 처리량): {}", String.format("%.1f", tps));
        log.info("  최종 조회수: {}/{}", result.getViewCount(), reducedRequests);
        log.info("  정확성: {}%", String.format("%.1f", result.getViewCount() * 100.0 / reducedRequests));
        log.info("");
    }

    private Post createTestPost(String title) {
        log.debug("Creating post with User ID: {}, Board ID: {}", testUser.getId(), testBoard.getId());
        Post post = Post.builder()
                .title(title + "_" + System.currentTimeMillis())
                .content("테스트 내용")
                .author(testUser)
                .board(testBoard)
                .viewCount(0)
                .build();
        Post savedPost = postRepository.save(post);
        log.debug("Saved Post ID: {}", savedPost.getId());
        return savedPost;
    }
}
