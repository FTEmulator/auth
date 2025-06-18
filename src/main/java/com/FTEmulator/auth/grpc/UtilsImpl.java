package com.FTEmulator.auth.grpc;

import org.springframework.grpc.server.service.GrpcService;

import com.FTEmulator.auth.grpc.UtilsOuterClass.AuthStatusResponse;

import io.grpc.stub.StreamObserver;

@GrpcService
public class UtilsImpl extends UtilsGrpc.UtilsImplBase {

    // Status
    @Override
    public void authStatus(UtilsOuterClass.AuthStatusRequest request, StreamObserver<AuthStatusResponse> responseObserver) {
        UtilsOuterClass.AuthStatusResponse response = AuthStatusResponse.newBuilder().setOk(true).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
