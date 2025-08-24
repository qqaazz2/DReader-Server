package com.example.DReaderServer.filter;

import com.example.DReaderServer.common.ResultResponse;
import com.example.DReaderServer.entity.User;
import com.example.DReaderServer.entity.LoginUser;
import com.example.DReaderServer.enums.ExceptionEnum;
import com.example.DReaderServer.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@AllArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final RedisTemplate redisTemplate;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = tokenService.getRequestToken(request);
            if ("/user/login".equals(request.getRequestURI()) || "/user/code".equals(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }
            if (token == null || token.trim().isEmpty()) throw new AuthenticationException("请先登录");

            String userName = tokenService.getUserNameFromToken(token);
            String key = tokenService.getTokenKey(userName);
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
