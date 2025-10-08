/*
 * ftemulator - ftemulator is a high-performance stock market investment simulator designed with extreme technical efficiency
 * 
 * Copyright (C) 2025-2025 Álex Frías (alexwebdev05)
 * Licensed under GNU Affero General Public License v3.0
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * For commercial licensing inquiries, please contact: alexwebdev05@proton.me
 * GitHub: https://github.com/alexwebdev05
 */

package com.ftemulator.auth.grpc;

import java.security.Key;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.grpc.server.service.GrpcService;

import com.ftemulator.auth.grpc.AuthOuterClass.*;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

@GrpcService
public class UtilsImpl extends AuthGrpc.AuthImplBase {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Key SIGNING_KEY = initializeSigningKey();

    private static final long TOKEN_EXPIRATION_SECONDS = 1_296_000L; // 15 days

    // Initialize signing key from environment variable or generate a new one
    private static Key initializeSigningKey() {

        // Check for JWT_SECRET environment variable
        String envSecret = System.getenv("JWT_SECRET");

        // If provided, use it as the signing key
        if (envSecret != null && !envSecret.isBlank()) {
            System.out.println("[SECURITY] Using JWT_SECRET from environment.");
            return Keys.hmacShaKeyFor(envSecret.getBytes());
        }

        // Otherwise, generate a new random key at startup
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        String generated = Base64.getEncoder().encodeToString(randomBytes);

        // Log a warning since this key will not persist across restarts
        System.out.println("[SECURITY] Generated new ephemeral JWT signing key at startup.");
        return Keys.hmacShaKeyFor(generated.getBytes());
    }

    // Auth status
    @Override
    public void authStatus(AuthStatusRequest request, StreamObserver<AuthStatusResponse> responseObserver) {
        AuthStatusResponse response = AuthStatusResponse.newBuilder()
                .setOk(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Create JWT token
    @Override
    public void createToken(CreateTokenRequest request, StreamObserver<CreateTokenResponse> responseObserver) {
        try {
            // Prepare token data
            Instant now = Instant.now();
            Date issuedAt = Date.from(now);
            Date expiresAt = Date.from(now.plusSeconds(TOKEN_EXPIRATION_SECONDS));

            String userId = request.getUserId();
            String ipAddress = request.getIpAddress();
            String sessionType = request.getSessionType();

            // Create JWT
            String token = Jwts.builder()
                    .setSubject(userId)
                    .claim("userId", userId)
                    .claim("ipAddress", ipAddress)
                    .claim("sessionType", sessionType)
                    .setIssuedAt(issuedAt)
                    .setExpiration(expiresAt)
                    .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                    .compact();

            // Store token info in Redis
            String redisKey = "token:" + token;
            String redisValue = String.format("%s|%s|%s", userId, ipAddress, sessionType);

            // Set with expiration
            redisTemplate.opsForValue().set(redisKey, redisValue, TOKEN_EXPIRATION_SECONDS, TimeUnit.SECONDS);

            // Return token
            CreateTokenResponse response = CreateTokenResponse.newBuilder()
                    .setToken(token)
                    .build();
            // Send response
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Error creating JWT: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    // Verify JWT token
    @Override
    public void verifyToken(VerifyTokenRequest request, StreamObserver<VerifyTokenResponse> responseObserver) {
        try {
            String token = request.getToken();

            // Parse and validate JWT
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(SIGNING_KEY)
                    .build()
                    .parseClaimsJws(token);

            // Extract claims
            Claims body = jws.getBody();

            // Check expiration
            if (body.getExpiration().before(new Date())) {
                throw new JwtException("Token expired");
            }

            // Extract required fields
            String userId = body.get("userId", String.class);
            String ipAddress = body.get("ipAddress", String.class);
            String sessionType = body.get("sessionType", String.class);

            // Check Redis to ensure token is still valid
            String redisKey = "token:" + token;
            String storedValue = redisTemplate.opsForValue().get(redisKey);

            // If not found, token is revoked or expired
            if (storedValue == null) {
                throw new JwtException("Token not found in Redis (possibly revoked or expired)");
            }

            // Verify stored data matches token claims to prevent tampering
            String expected = String.format("%s|%s|%s", userId, ipAddress, sessionType);
            if (!storedValue.equals(expected)) {
                throw new JwtException("Token data mismatch with Redis (possible tampering)");
            }

            // Return verification success
            VerifyTokenResponse response = VerifyTokenResponse.newBuilder()
                    .setUserId(userId)
                    .setSessionType(sessionType)
                    .setIpAddress(ipAddress)
                    .build();

            // Send response
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (ExpiredJwtException e) {
            responseObserver.onError(
                Status.UNAUTHENTICATED
                    .withDescription("Token expired")
                    .asRuntimeException()
            );
        } catch (JwtException e) {
            responseObserver.onError(
                Status.UNAUTHENTICATED
                    .withDescription("Invalid token: " + e.getMessage())
                    .asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Error verifying JWT: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }
}
