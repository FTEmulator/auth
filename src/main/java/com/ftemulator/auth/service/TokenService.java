package com.ftemulator.auth.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    public TokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveToken(String token, Map<String, String> tokenData) {
        // Guardar hash
        redisTemplate.opsForHash().putAll(token, tokenData);

        // Guardar token en set del usuario
        String userId = tokenData.get("userId");
        redisTemplate.opsForSet().add("user:" + userId + ":tokens", token);

        // Poner TTL
        redisTemplate.expire(token, 3600, TimeUnit.SECONDS);
    }
}
