package grpify.grpify.user.dto;

import grpify.grpify.common.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
public class SignUpDTO {
    private String name;
    private String email;
    private String profileImageUrl;
}
