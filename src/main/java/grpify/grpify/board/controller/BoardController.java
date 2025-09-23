package grpify.grpify.board.controller;

import grpify.grpify.auth.CustomUserDetails;
import grpify.grpify.board.dto.BoardCreateRequest;
import grpify.grpify.board.dto.BoardRequest;
import grpify.grpify.board.dto.BoardResponse;
import grpify.grpify.board.dto.BoardUpdateRequest;
import grpify.grpify.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    /**
     * 게시판 생성 (Command) - 관리자만 가능하도록 설정하는 것을 권장
     */
    @PostMapping
    // @PreAuthorize("hasRole('ADMIN')") // 메서드 시큐리티를 활성화했다면 사용 가능
    public ResponseEntity<Void> createBoard(
            @RequestBody BoardRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 관리자 권한 검증 로직 필요 (현재는 생략)
        Long newBoardId = boardService.create(request);
        URI location = URI.create("/api/boards/" + newBoardId);
        return ResponseEntity.created(location).build();
    }

    /**
     * 모든 게시판 목록 조회 (Query) - 누구나 가능
     */
    @GetMapping
    public ResponseEntity<List<BoardResponse>> findAllBoards() {
        List<BoardResponse> response = boardService.findAll();
        return ResponseEntity.ok(response);
    }

    /**
     * 게시판 단건 조회 (Query) - 누구나 가능
     */
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> findBoard(@PathVariable Long boardId) {
        BoardResponse response = boardService.findById(boardId);
        return ResponseEntity.ok(response);
    }


    /**
     * 게시판 수정 (Command) - 관리자만 가능하도록 설정하는 것을 권장
     */
    @PutMapping("/{boardId}")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boardService.updateBoard(boardId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 게시판 삭제 (Command) - 관리자만 가능하도록 설정하는 것을 권장
     */
    @DeleteMapping("/{boardId}")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 이 메서드는 하위 게시글, 댓글까지 연쇄적으로 소프트 삭제
        boardService.softDeleteBoardAndContents(boardId);
        return ResponseEntity.noContent().build();
    }
}