package grpify.grpify.auth;

import grpify.grpify.user.domain.User;
import grpify.grpify.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 클라이언트의 모든 requset에 대해 JWT 토큰을 검사하고,
 * 유효한 경우 인증(Authentication) 객체를
 * Spring Security의 SecurityContext에 등록하는 필터.
 * OncePerRequestFilter를 상속하여, 요청 당 한 번만 실행되도록 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 요청 헤더에서 JWT를 추출한다.
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String userIdStr = jwtTokenProvider.getUserId(token); //subject

            // DB 에서 user 조회한 후 UserDetails 로 감싸줌
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(userIdStr);

            // Authentication 객체 생성
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // SecurityContext 에 등록
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        // 다음 필터로 요청을 전달한다.
        filterChain.doFilter(request, response);
    }

    /**
     * HttpServletRequest의 Authorization 헤더에서 'Bearer_' 접두사 제거
     * 순수한 JWT 문자열 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // 헤더가 존재하고, 'Bearer '로 시작하는지 확인
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 'Bearer(+공백)' 다음의 문자열(7번째 인덱스부터)을 반환
            return bearerToken.substring(7);
        }
        return null;
    }
}