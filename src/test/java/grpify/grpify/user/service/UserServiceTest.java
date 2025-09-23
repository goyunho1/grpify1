package grpify.grpify.user.service;

import grpify.grpify.common.enums.Role;
import grpify.grpify.common.exception.DuplicateException;
import grpify.grpify.user.domain.User;
import grpify.grpify.user.dto.SignUpDTO;
import grpify.grpify.user.dto.UserResponse;
import grpify.grpify.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class) //beforeEach 역할 포함
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private SignUpDTO signUpDTO;
    private User mockUser;
    private final Long userId = 1L;
    @BeforeEach // 각 테스트 메서드 실행 전에 초기화
    void setUp() {
        signUpDTO = SignUpDTO.builder()
                .name("testUser")
                .email("test@example.com")
                .build();

        mockUser = User.builder()
                .id(1L)
                .name(signUpDTO.getName())
                .email(signUpDTO.getEmail())
                .build();
    }

    @Test
    @DisplayName("회원가입 성공 - 중복 없음")
    void signUp_Success() {
        // Given (준비)
        // userRepository.existsByname() 호출 시 false 반환하도록 모의 설정
        given(userRepository.existsByEmail(signUpDTO.getEmail())).willReturn(false);
        given(userRepository.existsByName(signUpDTO.getName())).willReturn(false);
        // userRepository.save() 호출 시,
        // '어떤 User 객체'가 넘어와도 '가짜 ID가 설정된 User 객체'를 반환하도록 모의 설정
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        // When (실행)
        User savedUser = userService.signUp(signUpDTO);

        // Then (검증)
        assertThat(savedUser).isNotNull(); // 저장된 User 객체가 null이 아닌지
        assertThat(savedUser.getId()).isEqualTo(userId); // ID가 설정되었는지
        assertThat(savedUser.getName()).isEqualTo(signUpDTO.getName()); // 이름이 일치하는지
        assertThat(savedUser.getEmail()).isEqualTo(signUpDTO.getEmail());
        assertThat(savedUser.getRole()).isEqualTo(Role.USER); // 기본 역할 확인
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    void signUp_fail_email() {
        //given
        given(userRepository.existsByEmail(signUpDTO.getEmail())).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(signUpDTO))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("이미 사용 중인 이메일입니다: " + signUpDTO.getEmail()); // 메시지 검증

        // verify:
        verify(userRepository).existsByEmail(signUpDTO.getEmail());
        verify(userRepository, never()).existsByName(signUpDTO.getName());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이름")
    void signUp_fail_name() {
        //given
        given(userRepository.existsByEmail(signUpDTO.getEmail())).willReturn(false);
        given(userRepository.existsByName(signUpDTO.getName())).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(signUpDTO))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("이미 사용 중인 이름입니다: " + signUpDTO.getName()); // 메시지 검증

        // verify:
        verify(userRepository).existsByEmail(signUpDTO.getEmail());
        verify(userRepository).existsByName(signUpDTO.getName());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findById_success() {
        //given
        given(userRepository.findById(anyLong())).willReturn(Optional.of(mockUser));

        //when
        UserResponse foundUser = userService.findById(userId);

        //then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(userId);
        assertThat(foundUser.getName()).isEqualTo("testUser");

        //verify
        verify(userRepository).findById(userId);
    }

    @Test
    void findByName() {
    }
}