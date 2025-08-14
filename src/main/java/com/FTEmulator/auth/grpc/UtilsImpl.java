/*
 * FTEmulator - FTEmulator is a high-performance stock market investment simulator designed with extreme technical efficiency
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
package com.FTEmulator.auth.grpc;

import org.springframework.grpc.server.service.GrpcService;

import com.FTEmulator.auth.grpc.AuthOuterClass.AuthStatusRequest;
import com.FTEmulator.auth.grpc.AuthOuterClass.AuthStatusResponse;

import io.grpc.stub.StreamObserver;

@GrpcService
public class UtilsImpl extends AuthGrpc.AuthImplBase {

    // Status
    @Override
    public void authStatus(AuthStatusRequest request, StreamObserver<AuthStatusResponse> responseObserver) {
        AuthStatusResponse response = AuthStatusResponse
            .newBuilder()
            .setOk(true).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
