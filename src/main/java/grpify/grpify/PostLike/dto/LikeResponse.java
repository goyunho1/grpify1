package grpify.grpify.PostLike.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeResponse {
    private int likeCount;
    private boolean isLiked;
}
