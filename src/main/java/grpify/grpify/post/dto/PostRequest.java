package grpify.grpify.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequest {
    private Long id;
    
    @NotNull(message = "게시판 ID는 필수입니다.")
    private Long boardId;
    
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;
    
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 10000, message = "내용은 10000자를 초과할 수 없습니다.")
    private String content;
}
