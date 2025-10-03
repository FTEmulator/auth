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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.grpc.server.service.GrpcService;

import com.ftemulator.auth.grpc.AuthOuterClass.AuthStatusRequest;
import com.ftemulator.auth.grpc.AuthOuterClass.AuthStatusResponse;
import com.ftemulator.auth.grpc.AuthOuterClass.CreateTokenRequest;
import com.ftemulator.auth.grpc.AuthOuterClass.CreateTokenResponse;
import com.ftemulator.auth.grpc.AuthOuterClass.VerifyTokenRequest;
import com.ftemulator.auth.grpc.AuthOuterClass.VerifyTokenResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@GrpcService
public class UtilsImpl extends AuthGrpc.AuthImplBase {

    @Autowired
    private StringRedisTemplate redisTemplate;


    // Status
    @Override
    public void authStatus(AuthStatusRequest request, StreamObserver<AuthStatusResponse> responseObserver) {
        AuthStatusResponse response = AuthStatusResponse
            .newBuilder()
            .setOk(true).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void verifyToken(VerifyTokenRequest request, StreamObserver<VerifyTokenResponse> responseObserver) {
        try {
            String token = request.getToken();
            Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(token);

            VerifyTokenResponse response;
            if (!tokenData.isEmpty() && "ACTIVE".equals(tokenData.get("status"))) {
                response = VerifyTokenResponse.newBuilder()
                    .setUserId((String) tokenData.get("userId"))
                    .setSessionType((String) tokenData.get("sessionType"))
                    .setIpAddress((String) tokenData.get("ipAddress"))
                    .build();
            } else {
                response = VerifyTokenResponse.newBuilder().build();
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error verifying token: " + e.getMessage())
                .asRuntimeException());
        }
    }

    // Create token
    @Override
    public void createToken(CreateTokenRequest request, StreamObserver<CreateTokenResponse> responseObserver) {
        try {
            String userKey = "user:" + request.getUserId() + ":tokens";

            // Check if this device already has a token
            Set<String> userTokens = redisTemplate.opsForSet().members(userKey);
            System.out.println("DEBUG: userKey = " + userKey);
            System.out.println("DEBUG: userTokens = " + userTokens);
            System.out.println("DEBUG: request IP = " + request.getIpAddress());
            System.out.println("DEBUG: request sessionType = " + request.getSessionType());
            
            if (userTokens != null && !userTokens.isEmpty()) {
                for (String token : userTokens) {
                    Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(token);
                    System.out.println("DEBUG: Checking token = " + token);
                    System.out.println("DEBUG: tokenData = " + tokenData);
                    
                    // Convert Object to String for proper comparison
                    String storedIpAddress = tokenData.get("ipAddress") != null ? tokenData.get("ipAddress").toString() : null;
                    String storedSessionType = tokenData.get("sessionType") != null ? tokenData.get("sessionType").toString() : null;
                    
                    System.out.println("DEBUG: storedIpAddress = [" + storedIpAddress + "]");
                    System.out.println("DEBUG: storedSessionType = [" + storedSessionType + "]");
                    System.out.println("DEBUG: IP match = " + request.getIpAddress().equals(storedIpAddress));
                    System.out.println("DEBUG: Session match = " + request.getSessionType().equals(storedSessionType));
                    
                    if (request.getIpAddress().equals(storedIpAddress) 
                        && request.getSessionType().equals(storedSessionType)) {
                        System.out.println("DEBUG: MATCH FOUND! Returning existing token");
                        // Already exists a token for this device, return it
                        CreateTokenResponse response = CreateTokenResponse.newBuilder()
                            .setToken(token)
                            .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                        return;
                    }
                }
            }

            System.out.println("DEBUG: No match found, creating new token");
            // Token
            String token = UUID.randomUUID().toString();

            // Dates
            Instant createdAt = Instant.now();
            long ttlSeconds = 1296000; // 15 days
            Instant expiresAt = createdAt.plusSeconds(ttlSeconds);

            // Create hash token
            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("userId", request.getUserId());
            tokenData.put("ipAddress", request.getIpAddress());
            tokenData.put("sessionType", request.getSessionType());
            tokenData.put("createdAt", createdAt.toString());
            tokenData.put("expiresAt", expiresAt.toString());
            tokenData.put("status", "ACTIVE");
            tokenData.put("lastUsedAt", createdAt.toString());

            // Save on redis
            redisTemplate.opsForHash().putAll(token, tokenData);
            redisTemplate.expire(token, ttlSeconds, TimeUnit.SECONDS);

            // Bind token with userId
            redisTemplate.opsForSet().add(userKey, token);

            // Return token
            CreateTokenResponse response = CreateTokenResponse.newBuilder()
                .setToken(token)
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Error to create token: " + e.getMessage())
                .asRuntimeException());
        }
    }
}
