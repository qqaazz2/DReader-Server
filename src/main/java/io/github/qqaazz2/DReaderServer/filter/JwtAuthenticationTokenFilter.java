package io.github.qqaazz2.DReaderServer.filter;

import io.github.qqaazz2.DReaderServer.service.TokenService;
import io.github.qqaazz2.DReaderServer.common.ResultResponse;
import io.github.qqaazz2.DReaderServer.entity.User;
import io.github.qqaazz2.DReaderServer.entity.LoginUser;
import io.github.qqaazz2.DReaderServer.enums.ExceptionEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final RedisTemplate redisTemplate;
    private final List<String> passList = List.of("/api/user/login", "/api/user/code", "/api/image/system/view");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = tokenService.getRequestToken(request);
            if (passList.contains(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }

            if (token == null || token.trim().isEmpty()) throw new AuthenticationException("请先登录");

            String userEmail = tokenService.getUserNameFromToken(token);
            String key = tokenService.getTokenKey(userEmail);
            User user = (User) redisTemplate.opsForValue().get(key);
            if (user == null) {
                throw new AuthenticationException("用户登录已过期");
            }

            LoginUser loginUser = new LoginUser();
            loginUser.setUser(user);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, null);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticationToken);
            SecurityContextHolder.setContext(context);

            RequestAttributeSecurityContextRepository repository = new RequestAttributeSecurityContextRepository();
            repository.saveContext(context, request, response);

            tokenService.verifyToken(user);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException exception) {
            ObjectMapper mapper = new ObjectMapper();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println(mapper.writeValueAsString(ResultResponse.error(ExceptionEnum.NOT_AUTHORITY)));
            exception.printStackTrace();
        }
    }
}
