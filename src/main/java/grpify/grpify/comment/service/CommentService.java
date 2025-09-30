package grpify.grpify.comment.service;

import grpify.grpify.comment.domain.Comment;
import grpify.grpify.comment.dto.*;
import grpify.grpify.comment.repository.CommentRepository;
import grpify.grpify.commentLike.domain.CommentLike;
import grpify.grpify.commentLike.dto.LikeResponse;
import grpify.grpify.commentLike.repository.CommentLikeRepository;
import grpify.grpify.common.exception.NotFoundException;
import grpify.grpify.post.domain.Post;
import grpify.grpify.post.dto.PostResponse;
import grpify.grpify.post.service.PostService;
import grpify.grpify.user.domain.User;
import grpify.grpify.user.dto.UserResponse;
import grpify.grpify.user.repository.UserRepository;
import grpify.grpify.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
//@SQLRestriction() isDeleted = true 인 객체도 포함해야 함 -> page 는 native 로 해서 상관없을 수도??
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostService postService;
    private final UserService userService;

    @Transactional(readOnly = true) // 리턴 타입 page, list 고민해보기
    public Page<CommentsResponse> findCommentsByPost(Long postId, Long currentUserId, Pageable pageable) {

//      Page<CommentQueryDto> queryResultPage = commentRepository.findCommentHierarchyByPostId(postId, pageable);
        Page<CommentQueryDto> queryResultPage = commentRepository.findCommentsByPostId(postId, pageable);
        if (queryResultPage.getContent().isEmpty()) {
            // 빈 리스트 담긴 Page 객체 리턴
            return Page.empty(pageable);
        }

        // commentId 추출
        List<Long> commentIds = queryResultPage.stream()
                .map(CommentQueryDto::getCommentId)
                .toList();

        // SELECT cl.comment.id
        // FROM CommentLike cl
        // WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds
        // currentUser 의 댓글 좋아요 set 가져와서 현재 게시물 댓글들과 대조
        Set<Long> likedCommentIds = commentLikeRepository.findLikedComments(currentUserId, commentIds);

        // 생성자 메서드에 is_deleted 체크 로직 포함
        return queryResultPage.map(dto ->
            CommentsResponse.from(dto, likedCommentIds.contains(dto.getCommentId()))
        );

    }


    /**
     * 댓글 작성 후에 리턴 타입, 리디렉션 시나리오 생각해서 변경해보기
     * PostService 처럼 읽기 쓰기 책임 분리
     * -> CommentResponse로 쓰기 작업(write, update) 리턴하지 않고, get 요청을 통해 최신 데이터 보장하도록
     *
     * 댓글을 달았을 때 현재 보고있는 페이지와 댓글이 달린 위치가 달라질 수 있음.
     * -> 리디렉션 해줘야됨, 댓글이 달린 pageNumber, commentId 만 넘겨주면 프론트에서 찾아가고 시각적 효과까지 줄 수 있을 듯?
     *
     */
    @Transactional
    public CommentWriteResponse write(CommentRequest request, Long userId) {

        Post post = postService.findById(request.getPostId());
        User author = userService.findById(userId);

        Comment parent = null;
        int depth = 0;
        String sortKey = "";

        if (request.getParentCommentId() != null) {
            parent = findById(request.getParentCommentId());
            depth = parent.getDepth() + 1;
        }

        Comment newComment = Comment.builder()
                .content(request.getContent())
                .sortKey(sortKey) // save 후에 commentId 생성돼서 임시로
                .depth(depth)
                .post(post)
                .author(author)
                .parentComment(parent)
                .build();

        commentRepository.save(newComment);

        String selfKey = String.format("%010d", newComment.getId());
        sortKey = (parent != null) ? parent.getSortKey() + "->" + selfKey : selfKey;

        newComment.setSortKey(selfKey); // 빌더로 다시 객체 생성할지 setter 사용할지 고민 필요.

        postService.incrementCommentCount(post.getId());

        // 이 댓글보다 앞에 있는 댓글의 수를 세어서 페이지 번호 계산
        long rank = commentRepository.countByPost_IdAndSortKeyLessThanEqual(post.getId(), sortKey);
        int pageSize = 20; // 설정값
        int pageNumber = (int) ((rank - 1) / pageSize);

        return CommentWriteResponse.from(pageNumber, newComment.getId());
    }

    @Transactional
    public CommentWriteResponse update(CommentRequest request, Long commentId, Long authorId) {
        Comment comment = findById(commentId);

        if (!comment.getAuthor().getId().equals(authorId)) {
            throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
        }

        comment.update(request.getContent());

        // 수정된 댓글의 위치(페이지 번호) 계산
        Post post = comment.getPost();
        String sortKey = comment.getSortKey();
        
        long rank = commentRepository.countByPost_IdAndSortKeyLessThanEqual(post.getId(), sortKey);
        int pageSize = 20; // 설정값
        int pageNumber = (int) ((rank - 1) / pageSize);

        return CommentWriteResponse.from(pageNumber, comment.getId());
    }

    @Transactional
    public void softDelete(Long commentId, Long userId) {
        Comment comment = findById(commentId);

        //권한 확인 필요

        comment.softDelete();
        postService.decrementCommentCount(comment.getPost().getId());
    }


    /**
     * ui, db의 좋아요 여부가 불일치 할 경우를 고려.
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
    public LikeResponse like(Long userId, Long commentId, boolean shouldBeLiked) {

        User user = userService.findById(userId);
        Comment comment = findById(commentId);

        Optional<CommentLike> optionalLike = commentLikeRepository.findByUserAndComment(user, comment);
        boolean isLiked = optionalLike.isPresent();

        // Case 1: 현재 좋아요가 눌려있고(true) 좋아요 취소해야 함(false) -> 좋아요 취소
        if (isLiked && !shouldBeLiked) {
            commentLikeRepository.delete(optionalLike.get());
            comment.decrementLikeCount();
        }
        // Case 2: 현재 좋아요가 안 눌려있고(false) 좋아요 눌러야 함(true) -> 좋아요 추가
        else if(!isLiked && shouldBeLiked){
            CommentLike like = CommentLike.builder()
                    .user(user)
                    .comment(comment)
                    .build();

            commentLikeRepository.save(like);
            comment.incrementLikeCount();
        }
        // else case: 의도한대로 이미 저장되어있음 -> 아무것도 안하고 유지

        return LikeResponse.builder()
                .likeCount(comment.getLikeCount())
                .isLiked(shouldBeLiked)
                .build();
    }


    private Comment findById(Long commentId) {

        return commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다. ID: " + commentId));
    }

    /**
     * 게시판 삭제 시 해당 게시판의 모든 댓글을 일괄 소프트 삭제합니다.
     * @param boardId 삭제할 게시판의 ID
     */
    @Transactional
    public void bulkSoftDeleteByBoardId(Long boardId) {
        commentRepository.bulkSoftDeleteByBoardIdJpql(boardId); //<<<<<test 후 결정
        //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    }

    /**
     * 게시글 삭제 시 해당 게시글의 모든 댓글을 일괄 소프트 삭제합니다.
     * @param postId 삭제할 게시글의 ID
     */
    @Transactional
    public void bulkSoftDeleteByPostId(Long postId) {
        commentRepository.bulkSoftDeleteByPostId(postId);
    }
}