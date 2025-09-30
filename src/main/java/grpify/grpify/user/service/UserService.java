package grpify.grpify.user.service;

import grpify.grpify.common.exception.DuplicateException;
import grpify.grpify.user.domain.User;
import grpify.grpify.user.dto.SignUpDTO;
import grpify.grpify.user.dto.UserResponse;
import grpify.grpify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    //userResponse 구현해야댐
//    @Transactional
//    public User signUp(SignUpDTO signUpDTO) {
//        validateDuplicateMember(signUpDTO.getEmail(),signUpDTO.getName());
//
//        User newUser = User.builder()
//                        .name(signUpDTO.getName())
//                        .email(signUpDTO.getEmail())
//                        .profileImgUrl(signUpDTO.getProfileImageUrl())
//                        .build();
//
//        return userRepository.save(newUser);
//    }
//
//    private void validateDuplicateMember(String email, String name) {
//        Optional<User> foundUser = userRepository.findByName(name);
//        if (userRepository.existsByEmail(email)) {
//            throw new DuplicateException("이미 사용 중인 이메일입니다: " + email);
//        }
//        if (userRepository.existsByName(name)) {
//            throw new DuplicateException("이미 사용 중인 이름입니다: " + name);
//        }
//    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다. 사용자 ID: " + id));
    }

    public UserResponse findByName(String name) {
        return UserResponse.from(userRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다. 사용자 이름: " + name)));
    }
}
