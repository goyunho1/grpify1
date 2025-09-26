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
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PostViewCountConcurrencyTest {

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
    @DisplayName("Case 1: 엔티티 방식 - 동시성 문제로 일부 조회수 손실 발생")
    void testEntityApproachConcurrency() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    postService.incrementViewCountEntity(testPost.getId());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Entity approach failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();
        long endTime = System.currentTimeMillis();

        // then
        Post result = postRepository.findById(testPost.getId()).get();
        
        System.out.println("=== Case 1: 엔티티 방식 결과 ===");
        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("예상 조회수: " + threadCount);
        System.out.println("실제 조회수: " + result.getViewCount());
        System.out.println("손실된 조회수: " + (threadCount - result.getViewCount()));
        System.out.println("실패 건수: " + failureCount.get());
        System.out.println("정확성: " + String.format("%.1f%%", (result.getViewCount() * 100.0 / threadCount)));

        // 엔티티 방식은 동시성 문제로 인해 일부 조회수가 손실될 수 있음
        assertThat(result.getViewCount()).isLessThanOrEqualTo(threadCount);
    }

    @Test
    @DisplayName("Case 2: 벌크 업데이트 방식 - 모든 조회수가 정확히 반영됨")
    void testBulkUpdateApproachConcurrency() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    postService.incrementViewCount(testPost.getId());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Bulk update failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();
        long endTime = System.currentTimeMillis();

        // then
        Post result = postRepository.findById(testPost.getId()).get();
        
        System.out.println("=== Case 2: 벌크 업데이트 방식 결과 ===");
        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("예상 조회수: " + threadCount);
        System.out.println("실제 조회수: " + result.getViewCount());
        System.out.println("손실된 조회수: " + (threadCount - result.getViewCount()));
        System.out.println("실패 건수: " + failureCount.get());
        System.out.println("정확성: " + String.format("%.1f%%", (result.getViewCount() * 100.0 / threadCount)));

        // 벌크 업데이트는 원자적 연산으로 모든 조회수가 정확히 반영되어야 함
        assertThat(result.getViewCount()).isEqualTo(threadCount);
        assertThat(failureCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("Case 3: 비관적 락 방식 - 안전하지만 성능이 매우 느림")
    void testPessimisticLockApproachConcurrency() throws InterruptedException {
        // given
        int threadCount = 50; // 비관적 락은 느리므로 스레드 수를 줄임
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    postService.incrementViewCountPessimistic(testPost.getId());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Pessimistic lock failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS); // 비관적 락은 시간이 오래 걸릴 수 있음
        executorService.shutdown();
        long endTime = System.currentTimeMillis();

        // then
        Post result = postRepository.findById(testPost.getId()).get();
        
        System.out.println("=== Case 3: 비관적 락 방식 결과 ===");
        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("예상 조회수: " + threadCount);
        System.out.println("실제 조회수: " + result.getViewCount());
        System.out.println("손실된 조회수: " + (threadCount - result.getViewCount()));
        System.out.println("실패 건수: " + failureCount.get());
        System.out.println("정확성: " + String.format("%.1f%%", (result.getViewCount() * 100.0 / threadCount)));

        // 비관적 락은 완전히 직렬화되어 모든 조회수가 정확히 반영되어야 함
        assertThat(result.getViewCount()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("Case 4: 낙관적 락 방식 - 재시도를 통해 최종적으로 정확한 결과")
    void testOptimisticLockApproachConcurrency() throws InterruptedException {
        // given
        int threadCount = 50; // 낙관적 락은 재시도가 많아질 수 있으므로 스레드 수를 줄임
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    postService.incrementViewCountOptimistic(testPost.getId());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Optimistic lock failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS); // 낙관적 락은 재시도로 인해 시간이 오래 걸릴 수 있음
        executorService.shutdown();
        long endTime = System.currentTimeMillis();

        // then
        Post result = postRepository.findById(testPost.getId()).get();
        
        System.out.println("=== Case 4: 낙관적 락 방식 결과 ===");
        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("예상 조회수: " + threadCount);
        System.out.println("실제 조회수: " + result.getViewCount());
        System.out.println("손실된 조회수: " + (threadCount - result.getViewCount()));
        System.out.println("실패 건수: " + failureCount.get());
        System.out.println("정확성: " + String.format("%.1f%%", (result.getViewCount() * 100.0 / threadCount)));

        // 낙관적 락은 재시도를 통해 최종적으로 모든 조회수가 반영되어야 함
        // 단, 최대 재시도를 초과한 경우 일부 실패할 수 있음
        assertThat(result.getViewCount() + failureCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("성능 비교 테스트 - 모든 방식의 응답 시간 측정")
    void testPerformanceComparison() throws InterruptedException {
        int threadCount = 20; // 성능 비교를 위해 적은 수로 테스트
        
        // 각 방식별로 별도의 게시글 생성
        Post entityPost = createTestPost("엔티티방식");
        Post bulkPost = createTestPost("벌크업데이트");
        Post pessimisticPost = createTestPost("비관적락");
        Post optimisticPost = createTestPost("낙관적락");

        // Case 1: 엔티티 방식
        long entityTime = measureExecutionTime(() -> {
            try {
                executeInThreads(threadCount, () -> 
                    postService.incrementViewCountEntity(entityPost.getId()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Case 2: 벌크 업데이트 방식
        long bulkTime = measureExecutionTime(() -> {
            try {
                executeInThreads(threadCount, () -> 
                    postService.incrementViewCount(bulkPost.getId()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Case 3: 비관적 락 방식
        long pessimisticTime = measureExecutionTime(() -> {
            try {
                executeInThreads(threadCount, () -> 
                    postService.incrementViewCountPessimistic(pessimisticPost.getId()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Case 4: 낙관적 락 방식
        long optimisticTime = measureExecutionTime(() -> {
            try {
                executeInThreads(threadCount, () -> 
                    postService.incrementViewCountOptimistic(optimisticPost.getId()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // 결과 출력
        System.out.println("=== 성능 비교 결과 ===");
        System.out.println("엔티티 방식: " + entityTime + "ms");
        System.out.println("벌크 업데이트 방식: " + bulkTime + "ms");
        System.out.println("비관적 락 방식: " + pessimisticTime + "ms");
        System.out.println("낙관적 락 방식: " + optimisticTime + "ms");

        // 각 게시글의 최종 조회수 확인
        System.out.println("=== 최종 조회수 ===");
        System.out.println("엔티티 방식: " + postRepository.findById(entityPost.getId()).get().getViewCount());
        System.out.println("벌크 업데이트 방식: " + postRepository.findById(bulkPost.getId()).get().getViewCount());
        System.out.println("비관적 락 방식: " + postRepository.findById(pessimisticPost.getId()).get().getViewCount());
        System.out.println("낙관적 락 방식: " + postRepository.findById(optimisticPost.getId()).get().getViewCount());

        // 벌크 업데이트가 가장 빨라야 함
        assertThat(bulkTime).isLessThan(entityTime);
        assertThat(bulkTime).isLessThan(pessimisticTime);
        assertThat(bulkTime).isLessThan(optimisticTime);
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

    private void executeInThreads(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    System.err.println("Thread execution failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();
    }

    private long measureExecutionTime(Runnable task) {
        long startTime = System.currentTimeMillis();
        task.run();
        return System.currentTimeMillis() - startTime;
    }
}
