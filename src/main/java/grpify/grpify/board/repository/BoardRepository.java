package grpify.grpify.board.repository;

import grpify.grpify.board.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {
    Optional<Board> findByNameAndIsDeletedFalse(String boardName);
    Optional<Board> findByIdAndIsDeletedFalse(Long id);
    List<Board> findAllByIsDeletedFalse();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}

