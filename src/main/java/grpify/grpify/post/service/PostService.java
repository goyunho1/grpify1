package grpify.grpify.post.service;


import com.sun.source.doctree.CommentTree;
import grpify.grpify.PostLike.domain.PostLike;
import grpify.grpify.PostLike.repository.PostLikeRepository;
import grpify.grpify.board.domain.Board;
import grpify.grpify.board.dto.BoardResponse;
import grpify.grpify.board.repository.BoardRepository;
import grpify.grpify.board.service.BoardService;
import grpify.grpify.comment.domain.Comment;
import grpify.grpify.comment.repository.CommentRepository;
import grpify.grpify.comment.service.CommentService;
import grpify.grpify.commentLike.domain.CommentLike;
import grpify.grpify.PostLike.dto.LikeResponse;
import grpify.grpify.common.exception.NotFoundException;
import grpify.grpify.post.domain.Post;
import grpify.grpify.post.dto.PostRequest;
import grpify.grpify.post.dto.PostResponse;
import grpify.grpify.post.repository.PostRepository;
import grpify.grpify.user.domain.User;
import grpify.grpify.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Comments;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository; //postLikeService 따로 구현 않고 postservice 에서 직접 사용
    private final UserService userService;
//    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final BoardService boardService;

    public Post findById(Long postId) {
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
    }

    public Page<PostResponse> findByBoard(Long boardId, Pageable pageable) {
        Board board = boardService.findById(boardId);

        return postRepository.findByBoardAndIsDeletedFalse(board, pageable)
                .map(PostResponse::from);
    }

    public PostResponse read(Long postId, Long currentUserId) {
        Post post = findById(postId);
        boolean isLiked = false;

        if (currentUserId != null) {
            isLiked = postLikeRepository.existsByUser_IdAndPost(currentUserId, post);
        }

        // post.incrementViewCount()
        // 트랜잭션 길어짐, write 작업 분리! <<<<

        return PostResponse.from(post,isLiked);
        // read 메서드 외에는 isLiked 사용 안함
    }


    @Transactional
    public void incrementViewCount(Long postId) {
        // 벌크 업데이트로 동시성 문제 해결
        postRepository.incrementViewCount(postId);
    }

    @Transactional
    public PostResponse write(PostRequest request, Long boardId, User author) {
        
        // 게시판 존재 여부 확인
        Board board = boardService.findById(boardId);

        Post newPost = Post.builder()
                        .title(request.getTitle())
                        .content(request.getContent())
                        .board(board)
                        .author(author)
                        .build();

        postRepository.save(newPost);

        return PostResponse.from(newPost);
    }

    @Transactional
    public PostResponse update(PostRequest request, Long currentUserId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(request.getId())
                .orElseThrow(() -> new NotFoundException("수정하려는 게시글을 찾을 수 없습니다. ID: " + request.getId()));

        // 권한 확인 (작성자만 수정 가능)
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("게시글 수정 권한이 없습니다.");
        }

        post.update(request.getTitle(), request.getContent());

        return PostResponse.from(post);
    }

    @Transactional
    public void softDelete(Long postId) {
        Post post = findById(postId);

        // 하위 요소부터 처리
        commentRepository.bulkSoftDeleteByPostId(postId);

        post.softDelete();
    }
//      메서드 연쇄적으로 재사용 하고 싶지만 (board - post - comment),
//      N+1 문제 막기 위해 벌크 업데이트

    @Transactional
    public void bulkSoftDeleteByBoardId(Long boardId) {
        postRepository.bulkSoftDeleteByBoardIdNative(boardId);
    }

    @Transactional
    public void incrementCommentCount(Long postId) {
        Post post = findById(postId); //postId 대신 post 객체를 받아 오는 것 고려
        post.incrementCommentCount();
    }

    @Transactional
    public void decrementCommentCount(Long postId) {
        Post post = findById(postId);
        post.decrementCommentCount();
    }

    /**
     * ui, db의 좋아요 여부가 불일치 할 경우 고려.
     * 사용자가 좋아요를 누른 직후 UI에 바로 반영하고, 서버에는 비동기로 반영하는 방식으로 구현한다면?
     *
     * → ui 에는 좋아요 된 상태인데 db상에는 아니었다면?
     *
     *  (댓글과 좋아요 여부 나눠 받는 상황or 요청 짧은 시간이 여러번 보낸다면 or ui에만 반영되고 요청이 안갔다면)
     *
     * → 결국 사용자가 하려는 동작은 취소! → 어떤 구간이 틀렸든 취소 요청이 되도록 하면됨!
     *
     * → 토글 결과는 좋아요 로 등록돼버림. → 좋아요 api 요청시에 liked 도 같이 보내면 해결!
     */
    @Transactional
    public LikeResponse like(Long userId, Long postId, boolean shouldBeLiked) {

        User user = userService.findById(userId);
        Post post = findById(postId);

        Optional<PostLike> optionalLike = postLikeRepository.findByUserAndPost(user, post);
        boolean isLiked = optionalLike.isPresent();

        // Case 1: 현재 좋아요가 눌려있고(true) 좋아요 취소해야 함(false) -> 좋아요 취소
        if (isLiked && !shouldBeLiked) {
            postLikeRepository.delete(optionalLike.get());
            post.decrementLikeCount();
        }
        // Case 2: 현재 좋아요가 안 눌려있고(false) 좋아요 눌러야 함(true) -> 좋아요 추가
        else if(!isLiked && shouldBeLiked){
            PostLike like = PostLike.builder()
                    .user(user)
                    .post(post)
                    .build();

            postLikeRepository.save(like);
            post.incrementLikeCount();
        }
        // else case: 의도한대로 이미 저장되어있음 -> 아무것도 안하고 유지

        return LikeResponse.builder()
                .likeCount(post.getLikeCount())
                .isLiked(shouldBeLiked)
                .build();
    }

    // ==== 테스트용 조회수 증가 메서드들 ====
    
    // Case 1: 엔티티 방식 (SELECT → 메모리 증가 → UPDATE)
    @Transactional
    public void incrementViewCountEntity(Long postId) {
        Post post = postRepository.findById(postId).get(); // SELECT
        post.incrementViewCount();                          // 메모리 증가
        postRepository.save(post);                          // UPDATE
    }

    // Case 2: 벌크 업데이트 방식 (단일 UPDATE 쿼리) - 이미 구현되어 있음
    // incrementViewCount() 메서드가 이미 벌크 업데이트 방식

    // Case 3: 비관적 락 방식 (Pessimistic Lock)
    @Transactional
    public void incrementViewCountPessimistic(Long postId) {
        // SELECT ... FOR UPDATE로 Row Lock 즉시 획득
        Post post = postRepository.findByIdWithPessimisticLock(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
        post.incrementViewCount();
        postRepository.save(post);
        // 트랜잭션 종료 시 락 해제
    }

    // Case 3-1: 비관적 락 방식 (Native Query)
    @Transactional
    public void incrementViewCountPessimisticNative(Long postId) {
        // Native Query로 FOR UPDATE 사용
        Post post = postRepository.findByIdWithPessimisticLockNative(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
        post.incrementViewCount();
        postRepository.save(post);
    }

    // Case 4: 낙관적 락 방식 (수동 Version 체크)
    @Transactional
    public void incrementViewCountOptimistic(Long postId) {
        int maxRetries = 5;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                // 현재 조회수 확인
                Integer currentViewCount = postRepository.getCurrentViewCount(postId);
                if (currentViewCount == null) {
                    throw new NotFoundException("게시글을 찾을 수 없습니다. ID: " + postId);
                }
                
                // 조회수 증가 시도 (현재 값과 일치할 때만 업데이트)
                int updatedRows = postRepository.updateViewCountWithVersionCheck(
                    postId, currentViewCount, currentViewCount + 1);
                
                if (updatedRows > 0) {
                    return; // 성공
                } else {
                    // 다른 트랜잭션이 먼저 업데이트함 - 재시도 필요
                    throw new OptimisticLockingFailureException("조회수가 다른 트랜잭션에 의해 변경됨");
                }
                
            } catch (OptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RuntimeException("조회수 증가 실패: 최대 재시도 초과");
                }
                
                // 지수 백오프로 재시도 간격 조정
                try {
                    Thread.sleep(50 + (20 * attempt)); 
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생");
                }
            }
        }
    }
}
