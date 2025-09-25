package grpify.grpify.board.controller;

import grpify.grpify.auth.CustomUserDetails;
import grpify.grpify.board.domain.Board;
import grpify.grpify.board.dto.BoardRequest;
import grpify.grpify.board.dto.BoardResponse;
import grpify.grpify.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    /**
     * 게시판 목록 조회
     * 모든 사용자가 접근 가능 (비로그인 포함)
     */
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllBoards() {
        List<BoardResponse> boards = boardService.findAll();
        return ResponseEntity.ok(boards);
    }

    /**
     * 특정 게시판 조회
     * 모든 사용자가 접근 가능 (비로그인 포함)
     */
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long boardId) {
        Board board = boardService.findById(boardId);
        BoardResponse response = BoardResponse.from(board);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시판 생성
     * 관리자만 접근 가능
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardResponse> createBoard(
            @Valid @RequestBody BoardRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        BoardResponse createdBoard = boardService.create(request);
        URI location = URI.create("/api/boards/" + createdBoard.getId());
        return ResponseEntity.created(location).body(createdBoard);
    }

    /**
     * 게시판 수정
     * 관리자만 접근 가능
     */
    @PutMapping("/{boardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardResponse> updateBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        BoardResponse updatedBoard = boardService.update(boardId, request);
        return ResponseEntity.ok(updatedBoard);
    }

    /**
     * 게시판 삭제
     * 관리자만 접근 가능
     * 하위 게시글과 댓글도 함께 소프트 삭제됨
     */
    @DeleteMapping("/{boardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        boardService.softDelete(boardId);
        return ResponseEntity.noContent().build();
    }
}