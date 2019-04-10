package com.tyktechnologies.tykmiddleware;

import coprocess.DispatcherGrpc;
import coprocess.CoprocessObject;
import coprocess.CoprocessReturnOverrides;
import coprocess.CoprocessSessionState;

public class TykDispatcher extends DispatcherGrpc.DispatcherImplBase {

    @Override
    public void dispatch(CoprocessObject.Object request,
            io.grpc.stub.StreamObserver<CoprocessObject.Object> responseObserver) {

        System.out.println("*** Incoming Request ***");
        System.out.println(request.toString());

        CoprocessObject.Object modifiedRequest = null;

        switch (request.getHookName()) {
            case "MyPreMiddleware":
                modifiedRequest = MyPreHook(request);
                break;
            case "MyAuthHook":
                modifiedRequest = MyAuthHook(request);
                break;
            default:
            // Do nothing, the hook name isn't implemented!
        }

        // Return the modified request (if the transformation was done):
        if (modifiedRequest != null) {
            responseObserver.onNext(modifiedRequest);

            System.out.println("*** Transformed Request ***");
            System.out.println(modifiedRequest.toString());
        };

        responseObserver.onCompleted();
    }

    CoprocessObject.Object MyPreHook(CoprocessObject.Object request) {
        CoprocessObject.Object.Builder builder = request.toBuilder();
        builder.getRequestBuilder().putSetHeaders("customheader", "customvalue");
        return builder.build();
    }

    CoprocessObject.Object MyAuthHook(CoprocessObject.Object request) {
        String authHeader = request.getRequest().getHeadersOrDefault("Authorization", "");
        if(!authHeader.equals("0cc984058c452f207f788efab86c1293")) {
            CoprocessObject.Object.Builder builder = request.toBuilder();
            CoprocessReturnOverrides.ReturnOverrides retOverrides = CoprocessReturnOverrides.ReturnOverrides.newBuilder()
            .setResponseCode(403)
            .setResponseError("Not authorized")
            .build();

            builder.getRequestBuilder().setReturnOverrides(retOverrides);
            return builder.build();
        }

        CoprocessSessionState.SessionState session = CoprocessSessionState.SessionState.newBuilder()
        .setRate(1000.0)
        .setPer(1.0)
        .build();

        CoprocessObject.Object.Builder builder = request.toBuilder();
        builder.putMetadata("token", "0cc984058c452f207f788efab86c1293");
        builder.setSession(session);

        return builder.build();
    }
}
