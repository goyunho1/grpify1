package grpify.grpify.common.exception;

import lombok.Getter;

@Getter
public class DuplicateException extends RuntimeException {
    private final String errorCode;

    // 기본 생성자: 기본 메시지와 에러 코드를 설정합니다.
    public DuplicateException() {
        super("이미 사용 중인 이름입니다.");
        this.errorCode = "DUPLICATE";
    }

    public DuplicateException(String s) {
        super(s);
        this.errorCode = "DUPLICATE";
    }
}
