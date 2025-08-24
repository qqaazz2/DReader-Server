package com.example.DReaderServer.service;

import com.example.DReaderServer.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {
    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // 令牌有效期（默认30分钟）
    @Value("${token.expireTime}")
    private long expireTime;

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    public static final String LOGIN_USER_KEY = "u:login:";

    // token 前缀
    public static final String TOKEN_PREFIX = "Bearer ";

    // 20分钟时间
    private static final Long MILLIS_MINUTE_TEN = 20 * 60 * 1000L;

    //密钥实例
    private static SecretKey KEY;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    private void init(){
        KEY = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String createToken(User user) {
        refreshToken(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put(LOGIN_USER_KEY, user.getEmail());
        claims.put("TIME-DATE", System.currentTimeMillis());
        return createToken(claims);
    }

    public void refreshToken(User user) {
        String userKey = getTokenKey(user.getEmail());
        long currentTimeMillis = System.currentTimeMillis(); // 获取当前时间戳
        user.setExpireTime(new Date(currentTimeMillis));
        redisTemplate.opsForValue().set(userKey, user);
        redisTemplate.expire(userKey, expireTime, TimeUnit.MINUTES);
    }


    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public User getLoginUserFromRequest(HttpServletRequest request) {
        // 获取请求携带的令牌
        String token = getRequestToken(request);
        if (!token.isEmpty()) {
            // 解析 JWT 的 Token,获取username
            String userEmail = getUserNameFromToken(token);
            String userKey = getTokenKey(userEmail);
            return (User) redisTemplate.opsForValue().get(userKey);
        }
        return null;
    }


    /**
     * 验证令牌有效期，相差不足 20 分钟，自动刷新缓存
     *
     * @param loginUser 用户
     */
    public void verifyToken(User loginUser) {
        long expireTime = loginUser.getExpireTime().getTime();
        long currentTime = System.currentTimeMillis();
        // 相差不足 20 分钟，自动刷新缓存
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN) {
            refreshToken(loginUser);
        }
    }


    /**
     * 获取请求携带的令牌
     *
     * @param request
     * @return
     */
    public String getRequestToken(HttpServletRequest request) {
        String token = "";
        if(request.getHeader("Upgrade") != null && request.getHeader("Upgrade").equals("websocket")){
            token = request.getParameter(header);
        }else {
            token = request.getHeader(header);
        }

        if (token != null && !token.isEmpty() && token.startsWith(TOKEN_PREFIX)) {
            token = token.replace(TOKEN_PREFIX, "");
        }
        return token;
    }

    /**
     * 获取token redis key
     *
     * @return
     */
    public String getTokenKey(String userEmail) {
        return LOGIN_USER_KEY + userEmail;
    }

    /**
     * jwt生成token
     *
     * @param claims
     * @return
     */
    private String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
//                .expiration(new Date(System.currentTimeMillis() + expireTime * 60 * 1000))
                // 过期时间，使用redis控制,如果jwt设置过期时间，每次生成的token都不一样
//                .setExpiration(new Date(System.currentTimeMillis() + expireTime * 1000))
                .signWith(KEY, Jwts.SIG.HS256).compact();
    }

    /**
     * jwt解析token
     *
     * @param token
     * @return
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * jwt解析token 用户名
     *
     * @param token
     * @return
     */
    public String getUserNameFromToken(String token) {
        return (String) getClaimsFromToken(token).get(LOGIN_USER_KEY);
    }
}
