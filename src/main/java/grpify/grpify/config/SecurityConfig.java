package grpify.grpify.config;

import com.nimbusds.oauth2.sdk.auth.JWTAuthentication;
import grpify.grpify.auth.CustomOAuth2UserService;
import grpify.grpify.auth.JwtAuthenticationFilter;
import grpify.grpify.auth.JwtAuthenticationFilter;
import grpify.grpify.auth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login/**", "/oauth2/**").permitAll()
                        // ✨ --- 정적 자원 경로 허용 추가 ---
                        .requestMatchers(
                                "/",                  // 루트 경로
                                "/favicon.ico",       // 파비콘
                                "/css/**",            // CSS 파일 경로
                                "/js/**",             // JavaScript 파일 경로
                                "/image/**",          // 이미지 파일 경로
                                "/error",              // Spring Boot의 기본 에러 페이지
                                "/redirect.html",
                                "/main.html",
                                "/card.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

}
