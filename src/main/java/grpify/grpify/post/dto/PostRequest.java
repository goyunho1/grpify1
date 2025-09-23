package grpify.grpify.post.dto;

import lombok.Getter;

@Getter
public class PostRequest {
    private Long id;
    private String title;
    private String content;

}
