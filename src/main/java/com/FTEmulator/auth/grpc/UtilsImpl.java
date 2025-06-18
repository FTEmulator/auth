package com.FTEmulator.auth.grpc;

import org.springframework.grpc.server.service.GrpcService;

import com.FTEmulator.auth.grpc.UtilsOuterClass.AuthStatusResponse;
import com.FTEmulator.auth.grpc.UtilsOuterClass.AuthStatusRequest;

import io.grpc.stub.StreamObserver;

@GrpcService
public class UtilsImpl extends UtilsGrpc.UtilsImplBase {

    // Status
    @Override
    public void authStatus(AuthStatusRequest request, StreamObserver<AuthStatusResponse> responseObserver) {
        AuthStatusResponse response = AuthStatusResponse.newBuilder().setOk(true).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
