# 비관적 락 Native 쿼리 가이드

## 📋 JPA 어노테이션 vs Native 쿼리

### JPA 어노테이션 방식
```java
@Query("SELECT p FROM Post p WHERE p.id = :postId")
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Post> findByIdWithPessimisticLock(@Param("postId") Long postId);
```

### Native 쿼리 방식
```java
@Query(value = "SELECT * FROM post WHERE post_id = :postId FOR UPDATE", nativeQuery = true)
Optional<Post> findByIdWithPessimisticLockNative(@Param("postId") Long postId);
```

## 🗄️ 데이터베이스별 비관적 락 문법

### 1. MySQL / MariaDB
```sql
-- 기본 FOR UPDATE
SELECT * FROM post WHERE post_id = ? FOR UPDATE;

-- 타임아웃 설정 (MySQL 8.0+)
SELECT * FROM post WHERE post_id = ? FOR UPDATE NOWAIT;
SELECT * FROM post WHERE post_id = ? FOR UPDATE SKIP LOCKED;
```

### 2. PostgreSQL
```sql
-- 기본 FOR UPDATE
SELECT * FROM post WHERE post_id = ? FOR UPDATE;

-- 타임아웃 설정
SELECT * FROM post WHERE post_id = ? FOR UPDATE NOWAIT;
SELECT * FROM post WHERE post_id = ? FOR UPDATE SKIP LOCKED;

-- 다른 락 모드들
SELECT * FROM post WHERE post_id = ? FOR NO KEY UPDATE;
SELECT * FROM post WHERE post_id = ? FOR SHARE;
SELECT * FROM post WHERE post_id = ? FOR KEY SHARE;
```

### 3. Oracle
```sql
-- 기본 FOR UPDATE
SELECT * FROM post WHERE post_id = ? FOR UPDATE;

-- 타임아웃 설정 (5초)
SELECT * FROM post WHERE post_id = ? FOR UPDATE WAIT 5;

-- 즉시 반환 (대기하지 않음)
SELECT * FROM post WHERE post_id = ? FOR UPDATE NOWAIT;

-- 특정 컬럼만 락
SELECT * FROM post WHERE post_id = ? FOR UPDATE OF view_count;
```

### 4. SQL Server
```sql
-- 힌트를 사용한 배타적 락
SELECT * FROM post WITH (UPDLOCK, ROWLOCK) WHERE post_id = ?;

-- 더 강력한 락
SELECT * FROM post WITH (XLOCK, ROWLOCK) WHERE post_id = ?;

-- 페이지 레벨 락
SELECT * FROM post WITH (UPDLOCK, PAGLOCK) WHERE post_id = ?;
```

### 5. H2 Database (테스트용)
```sql
-- 기본 FOR UPDATE (MySQL 호환 모드)
SELECT * FROM post WHERE post_id = ? FOR UPDATE;
```

## 🔧 Repository 구현 예시

```java
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // MySQL/PostgreSQL/H2용
    @Query(value = "SELECT * FROM post WHERE post_id = :postId FOR UPDATE", 
           nativeQuery = true)
    Optional<Post> findByIdWithPessimisticLockMySQL(@Param("postId") Long postId);
    
    // PostgreSQL NOWAIT용
    @Query(value = "SELECT * FROM post WHERE post_id = :postId FOR UPDATE NOWAIT", 
           nativeQuery = true)
    Optional<Post> findByIdWithPessimisticLockNoWait(@Param("postId") Long postId);
    
    // Oracle용 (타임아웃 5초)
    @Query(value = "SELECT * FROM post WHERE post_id = :postId FOR UPDATE WAIT 5", 
           nativeQuery = true)
    Optional<Post> findByIdWithPessimisticLockOracle(@Param("postId") Long postId);
    
    // SQL Server용
    @Query(value = "SELECT * FROM post WITH (UPDLOCK, ROWLOCK) WHERE post_id = :postId", 
           nativeQuery = true)
    Optional<Post> findByIdWithPessimisticLockSQLServer(@Param("postId") Long postId);
}
```

## ⚡ 락 옵션별 동작 방식

### 1. FOR UPDATE (기본)
- **동작**: 해당 행에 배타적 락 설정
- **대기**: 다른 트랜잭션이 락을 해제할 때까지 대기
- **용도**: 일반적인 비관적 락

### 2. FOR UPDATE NOWAIT
- **동작**: 즉시 락 획득 시도, 실패 시 에러 발생
- **대기**: 대기하지 않음
- **용도**: 빠른 응답이 필요한 경우

### 3. FOR UPDATE SKIP LOCKED
- **동작**: 락이 걸린 행은 건너뛰고 결과 반환
- **대기**: 대기하지 않음
- **용도**: 큐 처리, 배치 작업

### 4. FOR SHARE / FOR KEY SHARE
- **동작**: 공유 락 설정 (읽기 허용, 쓰기 차단)
- **용도**: 읽기 일관성 보장

## 🚨 주의사항

### 1. 데드락 위험
```sql
-- 잘못된 예: 여러 테이블을 다른 순서로 락
-- Transaction 1
SELECT * FROM post WHERE id = 1 FOR UPDATE;
SELECT * FROM user WHERE id = 1 FOR UPDATE;

-- Transaction 2  
SELECT * FROM user WHERE id = 1 FOR UPDATE;
SELECT * FROM post WHERE id = 1 FOR UPDATE;
```

### 2. 성능 영향
- 락 대기로 인한 응답시간 증가
- 동시 처리량 감소
- 커넥션 풀 고갈 위험

### 3. 타임아웃 설정
```java
// application.yml
spring:
  jpa:
    properties:
      javax.persistence.lock.timeout: 5000  # 5초
      hibernate.dialect.lock.timeout: 5000
```

## 📊 성능 비교

| 방식 | 응답시간 | 정확성 | 동시성 | 복잡도 |
|------|----------|--------|--------|---------|
| JPA @Lock | 느림 | 100% | 낮음 | 낮음 |
| FOR UPDATE | 느림 | 100% | 낮음 | 중간 |
| FOR UPDATE NOWAIT | 빠름 | 높음* | 중간 | 중간 |
| 벌크 UPDATE | 빠름 | 100% | 높음 | 낮음 |

*실패 시 재시도 로직 필요

## 🎯 권장사항

1. **일반적인 경우**: 벌크 업데이트 사용
2. **강한 일관성 필요**: FOR UPDATE 사용
3. **빠른 응답 필요**: FOR UPDATE NOWAIT + 재시도
4. **배치 처리**: FOR UPDATE SKIP LOCKED 사용
