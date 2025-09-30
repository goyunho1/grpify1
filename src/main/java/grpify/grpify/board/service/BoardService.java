package grpify.grpify.board.service;

import grpify.grpify.board.domain.Board;
import grpify.grpify.board.dto.BoardRequest;
import grpify.grpify.board.dto.BoardResponse;
import grpify.grpify.board.repository.BoardRepository;
import grpify.grpify.comment.repository.CommentRepository;
import grpify.grpify.comment.service.CommentService;
import grpify.grpify.common.exception.NotFoundException;
import grpify.grpify.common.exception.DuplicateException;
import grpify.grpify.post.domain.Post;
import grpify.grpify.post.repository.PostRepository;
import grpify.grpify.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
//    private final PostService postService;
//    private final CommentService commentService; //순환 참조 문제 때문에 repository 통해서 sodfdelete 실행
    private final CommentRepository commentRepository;
    // 반환 타입 고민해보기
    @Transactional
    public BoardResponse create(BoardRequest request) {
        if (boardRepository.existsByName(request.getName())) {
            throw new DuplicateException("이미 사용 중인 이름입니다. :" + request.getName());
        }

        Board newBoard = Board.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Board savedBoard = boardRepository.save(newBoard);
        return BoardResponse.from(savedBoard);
    }

    @Transactional
    public void softDelete(Long boardId) {
        Board board = findById(boardId);

        // 하위 요소부터 처리 //순환 참조 문제 때문에 repository 통해서 sodfdelete 실행
        commentRepository.bulkSoftDeleteByBoardIdNative(boardId);
        postRepository.bulkSoftDeleteByBoardIdNative(boardId);

        board.softDelete();
    }

    public Board findById(Long boardId) {

        return boardRepository.findByIdAndIsDeletedFalse(boardId)
                .orElseThrow(() -> new NotFoundException("게시판을 찾을 수 없습니다. ID: " + boardId));
    }

    /**
     * 특정 이름으로 게시판을 조회합니다. (삭제되지 않은 게시판만)
     * @param boardName 조회할 게시판의 이름
     * @return 조회된 게시판 정보 DTO
     */
    public BoardResponse findByName(String boardName) {

        Board board = boardRepository.findByNameAndIsDeletedFalse(boardName)
                .orElseThrow(() -> new NotFoundException("게시판을 찾을 수 없습니다. 이름: " + boardName));
        return BoardResponse.from(board);
    }

    /**
     * 모든 게시판 목록을 조회합니다. (삭제되지 않은 게시판만)
     * 페이징 x
     */
    public List<BoardResponse> findAll() {

        return boardRepository.findAllByIsDeletedFalse()
                .stream()
                .map(BoardResponse::from)
                .toList();
    }

    /**
     *
     * 게시판 정보를 수정합니다.
     * @param boardId 수정할 게시판의 ID (pathvariable)
     * @param request 게시판 수정 요청 DTO
     * @return 수정된 게시판 정보 DTO
     */
    @Transactional
    public BoardResponse update(Long boardId, BoardRequest request) {
        //(**수정하려는) 게시판 존재 여부 확인 (삭제되지 않은 게시판만)
        Board board = boardRepository.findByIdAndIsDeletedFalse(boardId)
                .orElseThrow(() -> new NotFoundException("수정하려는 게시판을 찾을 수 없습니다. ID: " + boardId));

        // 이름 중복 검사
        if (!board.getName().equals(request.getName())) {
            //if (boardRepository.existsByName) ????
            // name 은 그대로 두고 description 만 바꾼다면 existsByName 으로 조건문 작성시 에러
            if (boardRepository.existsByNameAndIdNot(request.getName(), boardId)) {
                throw new DuplicateException("이미 사용 중인 게시판 이름입니다: " + request.getName());

            }
        }
        //dirty check -> save 안써도 자동으로 update 쿼리 날아감
        board.update(request.getName(), request.getDescription());
        // 수정 결과 보여주기 위해 리턴 값 필요
        return BoardResponse.from(board);
    }
}
