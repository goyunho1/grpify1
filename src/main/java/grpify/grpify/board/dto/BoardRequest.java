package grpify.grpify.board.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardRequest {
    private String name;
    private String description;
}
