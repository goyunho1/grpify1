package grpify.grpify.auth;

import grpify.grpify.OAuthAccount.domain.OAuthAccount;
import grpify.grpify.OAuthAccount.repository.OAuthAccountRepository;
import grpify.grpify.user.domain.User;
import grpify.grpify.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler  {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuthAccountRepository oAuthAccountRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        // 내 서비스 JWT
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());

        // redirect.html로 리다이렉트 + 토큰 전달
        String targetUrl = "/redirect.html?token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
