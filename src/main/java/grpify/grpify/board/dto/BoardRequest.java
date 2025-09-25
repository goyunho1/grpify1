package grpify.grpify.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardRequest {
    @NotBlank(message = "게시판 이름은 필수입니다.")
    @Size(max = 50, message = "게시판 이름은 50자를 초과할 수 없습니다.")
    private String name;
    
    @Size(max = 200, message = "설명은 200자를 초과할 수 없습니다.")
    private String description;
}
