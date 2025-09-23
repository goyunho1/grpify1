package grpify.grpify.auth;

import grpify.grpify.user.domain.User;
import grpify.grpify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // JWT 토큰에 저장된 subject(userId)를 기반으로 조회
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(Long.valueOf(userId)) /////asdad
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        return new CustomUserDetails(user);
    }
}