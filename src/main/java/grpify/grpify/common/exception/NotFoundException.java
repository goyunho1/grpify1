package grpify.grpify.common.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {
    private final String errorCode;

    public NotFoundException() {
        super("찾을 수 없습니다");
        this.errorCode = "NOT_FOUND";
    }
    public NotFoundException(String s) {
        super(s);
        this.errorCode = "NOT_FOUND";
    }
}
