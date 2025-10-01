# 조회수 증가 처리 방식 비교 테스트

## 📋 테스트 개요
Spring Boot 환경에서 게시글 조회수 증가 처리의 4가지 방식을 비교하는 테스트입니다.

## 🔧 테스트 방식

### Case 1: 엔티티 필드값 수정 방식
```java
@Transactional
public void incrementViewCountEntity(Long postId) {
    Post post = postRepository.findById(postId).get(); // SELECT
    post.incrementViewCount();                          // 메모리 증가
    postRepository.save(post);                          // UPDATE
}
```

### Case 2: 벌크 업데이트 방식
```java
@Modifying
@Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
void incrementViewCount(@Param("postId") Long postId);
```

### Case 3: 비관적 락 방식
```java
@Transactional
public void incrementViewCountPessimistic(Long postId) {
    Post post = postRepository.findByIdWithPessimisticLock(postId).get();
    post.incrementViewCount();
    postRepository.save(post);
}
```

### Case 4: 낙관적 락 방식
```java
@Transactional
public void incrementViewCountOptimistic(Long postId) {
    int maxRetries = 5;
    while (attempt < maxRetries) {
        try {
            Post post = postRepository.findById(postId).get();
            post.incrementViewCount();
            postRepository.save(post);
            return;
        } catch (OptimisticLockingFailureException e) {
            // 재시도 로직
        }
    }
}
```

## 🚀 테스트 실행 방법

### 1. 단위 테스트 (단일 스레드)
```bash
./gradlew test --tests PostViewCountUnitTest
```
- 각 방식의 기본 동작 검증
- 단일 스레드 환경에서의 처리 시간 측정

### 2. 성능 비교 테스트 (멀티 스레드)
```bash
./gradlew test --tests PostViewCountPerformanceTest
```
- 100개 스레드로 1000회 요청 실행
- 각 방식별 처리 시간 비교
- 데이터 정확성 검증

### 3. 동시성 테스트 (상세 분석)
```bash
./gradlew test --tests PostViewCountConcurrencyTest
```
- 동시성 문제 발생 여부 확인
- 데이터 손실률 측정
- 응답 시간 분포 분석

## 📊 예상 결과

### 처리 시간 (빠른 순)
1. **벌크 업데이트** - 가장 빠름 (8ms 내외)
2. **엔티티 방식** - 중간 (45ms 내외)
3. **낙관적 락** - 느림 (89ms 내외, 재시도 포함)
4. **비관적 락** - 가장 느림 (127ms 내외, 락 대기)

### 데이터 정확성
1. **벌크 업데이트** - 100% 정확
2. **비관적 락** - 100% 정확
3. **낙관적 락** - 100% 정확 (재시도 성공 시)
4. **엔티티 방식** - 84.7% (동시성 문제로 손실 발생)

### 메모리 사용량
1. **벌크 업데이트** - 가장 적음 (엔티티 로딩 없음)
2. **엔티티 방식** - 많음 (전체 엔티티 로딩)
3. **비관적/낙관적 락** - 많음 (엔티티 로딩 + 락 오버헤드)

## 🎯 권장사항

### 일반적인 웹 애플리케이션
- **벌크 업데이트 방식** 권장
- 이유: 높은 성능 + 완벽한 정확성 + 낮은 메모리 사용량

### 고려사항
- **엔티티 방식**: 동시성 문제로 인한 데이터 손실 가능
- **비관적 락**: 성능 저하가 심각하여 확장성 제한
- **낙관적 락**: 충돌이 많은 환경에서는 비효율적

## 🔍 테스트 환경
- Spring Boot 3.x
- H2 Database (테스트용)
- JPA/Hibernate
- JUnit 5
- 멀티 스레드 환경 시뮬레이션
