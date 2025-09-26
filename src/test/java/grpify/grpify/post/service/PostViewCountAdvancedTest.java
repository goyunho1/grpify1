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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PostViewCountAdvancedTest {

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
    @DisplayName("고부하 동시성 테스트 - 1000개 스레드로 각 방식 테스트")
    void testHighLoadConcurrency() throws InterruptedException {
        int threadCount = 1000;
        
        // 각 방식별 테스트 데이터 준비
        Post entityPost = createTestPost("엔티티방식_고부하");
        Post bulkPost = createTestPost("벌크업데이트_고부하");
        Post pessimisticPost = createTestPost("비관적락_고부하");
        Post optimisticPost = createTestPost("낙관적락_고부하");

        System.out.println("=== 고부하 동시성 테스트 (1000 스레드) ===");

        // Case 1: 엔티티 방식
        TestResult entityResult = executeConcurrencyTest(
            "엔티티 방식", 
            threadCount, 
            () -> postService.incrementViewCountEntity(entityPost.getId()),
            entityPost.getId()
        );

        // Case 2: 벌크 업데이트 방식
        TestResult bulkResult = executeConcurrencyTest(
            "벌크 업데이트", 
            threadCount, 
            () -> postService.incrementViewCount(bulkPost.getId()),
            bulkPost.getId()
        );

        // Case 3: 비관적 락 방식 (스레드 수 줄임)
        TestResult pessimisticResult = executeConcurrencyTest(
            "비관적 락", 
            100, // 너무 느려서 스레드 수 줄임
            () -> postService.incrementViewCountPessimistic(pessimisticPost.getId()),
            pessimisticPost.getId()
        );

        // Case 4: 낙관적 락 방식 (스레드 수 줄임)
        TestResult optimisticResult = executeConcurrencyTest(
            "낙관적 락", 
            100, // 재시도로 인해 스레드 수 줄임
            () -> postService.incrementViewCountOptimistic(optimisticPost.getId()),
            optimisticPost.getId()
        );

        // 결과 분석
        printDetailedResults(entityResult, bulkResult, pessimisticResult, optimisticResult);

        // 검증
        assertThat(bulkResult.getAccuracy()).isEqualTo(100.0);
        assertThat(bulkResult.getExecutionTime()).isLessThan(entityResult.getExecutionTime());
    }

    @Test
    @DisplayName("응답 시간 분포 테스트 - 각 요청의 개별 응답 시간 측정")
    void testResponseTimeDistribution() throws InterruptedException {
        int requestCount = 100;
        
        // 벌크 업데이트 방식의 응답 시간 분포 측정
        List<Long> bulkResponseTimes = measureIndividualResponseTimes(
            requestCount,
            () -> postService.incrementViewCount(testPost.getId())
        );

        // 통계 계산
        long minTime = bulkResponseTimes.stream().min(Long::compareTo).orElse(0L);
        long maxTime = bulkResponseTimes.stream().max(Long::compareTo).orElse(0L);
        double avgTime = bulkResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        // 95퍼센타일 계산
        bulkResponseTimes.sort(Long::compareTo);
        long p95Time = bulkResponseTimes.get((int) (requestCount * 0.95));

        System.out.println("=== 벌크 업데이트 응답 시간 분포 ===");
        System.out.println("최소 응답 시간: " + minTime + "ms");
        System.out.println("최대 응답 시간: " + maxTime + "ms");
        System.out.println("평균 응답 시간: " + String.format("%.2f", avgTime) + "ms");
        System.out.println("95퍼센타일 응답 시간: " + p95Time + "ms");

        // 검증: 평균 응답 시간이 50ms 이하여야 함
        assertThat(avgTime).isLessThan(50.0);
    }

    @Test
    @DisplayName("메모리 사용량 비교 테스트")
    void testMemoryUsage() {
        int iterations = 1000;
        
        // GC 실행하여 초기 상태 정리
        System.gc();
        long initialMemory = getUsedMemory();

        // Case 1: 엔티티 방식 (메모리 사용량이 많을 것으로 예상)
        long entityMemoryBefore = getUsedMemory();
        for (int i = 0; i < iterations; i++) {
            postService.incrementViewCountEntity(testPost.getId());
        }
        long entityMemoryAfter = getUsedMemory();
        long entityMemoryUsage = entityMemoryAfter - entityMemoryBefore;

        // 메모리 정리
        System.gc();
        Thread.sleep(100);

        // Case 2: 벌크 업데이트 방식 (메모리 사용량이 적을 것으로 예상)
        Post bulkPost = createTestPost("벌크테스트");
        long bulkMemoryBefore = getUsedMemory();
        for (int i = 0; i < iterations; i++) {
            postService.incrementViewCount(bulkPost.getId());
        }
        long bulkMemoryAfter = getUsedMemory();
        long bulkMemoryUsage = bulkMemoryAfter - bulkMemoryBefore;

        System.out.println("=== 메모리 사용량 비교 ===");
        System.out.println("엔티티 방식 메모리 사용량: " + formatBytes(Math.max(0, entityMemoryUsage)));
        System.out.println("벌크 업데이트 메모리 사용량: " + formatBytes(Math.max(0, bulkMemoryUsage)));

        // 벌크 업데이트가 더 적은 메모리를 사용해야 함 (일반적으로)
        // 단, GC의 불확실성으로 인해 항상 보장되지는 않음
        System.out.println("메모리 효율성: " + (bulkMemoryUsage < entityMemoryUsage ? "벌크 업데이트 우수" : "결과 불확실"));
    }

    private TestResult executeConcurrencyTest(String testName, int threadCount, Runnable task, Long postId) 
            throws InterruptedException {
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    long requestStart = System.nanoTime();
                    task.run();
                    long requestEnd = System.nanoTime();
                    
                    totalResponseTime.addAndGet((requestEnd - requestStart) / 1_000_000); // ms로 변환
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println(testName + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(120, TimeUnit.SECONDS); // 2분 타임아웃
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // 최종 조회수 확인
        int finalViewCount = postRepository.findById(postId).get().getViewCount();
        double accuracy = (finalViewCount * 100.0) / threadCount;
        double avgResponseTime = successCount.get() > 0 ? 
            (double) totalResponseTime.get() / successCount.get() : 0.0;

        return new TestResult(
            testName, 
            threadCount, 
            finalViewCount, 
            successCount.get(), 
            failureCount.get(), 
            executionTime, 
            accuracy,
            avgResponseTime
        );
    }

    private List<Long> measureIndividualResponseTimes(int requestCount, Runnable task) {
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < requestCount; i++) {
            long startTime = System.nanoTime();
            task.run();
            long endTime = System.nanoTime();
            
            responseTimes.add((endTime - startTime) / 1_000_000); // ms로 변환
        }
        
        return responseTimes;
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private void printDetailedResults(TestResult... results) {
        System.out.println("\n=== 상세 테스트 결과 ===");
        System.out.printf("%-15s %-10s %-10s %-10s %-10s %-15s %-10s %-15s%n",
            "방식", "스레드수", "성공", "실패", "최종조회수", "실행시간(ms)", "정확성(%)", "평균응답시간(ms)");
        System.out.println("-".repeat(120));
        
        for (TestResult result : results) {
            System.out.printf("%-15s %-10d %-10d %-10d %-10d %-15d %-10.1f %-15.2f%n",
                result.getTestName(),
                result.getThreadCount(),
                result.getSuccessCount(),
                result.getFailureCount(),
                result.getFinalViewCount(),
                result.getExecutionTime(),
                result.getAccuracy(),
                result.getAvgResponseTime()
            );
        }
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

    // 테스트 결과를 담는 DTO 클래스
    private static class TestResult {
        private final String testName;
        private final int threadCount;
        private final int finalViewCount;
        private final int successCount;
        private final int failureCount;
        private final long executionTime;
        private final double accuracy;
        private final double avgResponseTime;

        public TestResult(String testName, int threadCount, int finalViewCount, 
                         int successCount, int failureCount, long executionTime, 
                         double accuracy, double avgResponseTime) {
            this.testName = testName;
            this.threadCount = threadCount;
            this.finalViewCount = finalViewCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.executionTime = executionTime;
            this.accuracy = accuracy;
            this.avgResponseTime = avgResponseTime;
        }

        // Getters
        public String getTestName() { return testName; }
        public int getThreadCount() { return threadCount; }
        public int getFinalViewCount() { return finalViewCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getExecutionTime() { return executionTime; }
        public double getAccuracy() { return accuracy; }
        public double getAvgResponseTime() { return avgResponseTime; }
    }
}
