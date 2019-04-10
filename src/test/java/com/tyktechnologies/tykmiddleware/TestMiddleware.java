package com.tyktechnologies.tykmiddleware;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import coprocess.CoprocessObject;
import coprocess.CoprocessMiniRequestObject;
import coprocess.DispatcherGrpc;

import com.tyktechnologies.tykmiddleware.TykClient;

public class TestMiddleware {
    @Test
    public void testPreHook() throws Exception {
        GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

        DispatcherGrpc.DispatcherImplBase serviceImpl = mock(DispatcherGrpc.DispatcherImplBase.class,
        delegatesTo(new TykDispatcher()));
        
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful
        // shutdown.
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());

        // Create a client channel and register for automatic graceful shutdown.
        ManagedChannel channel = grpcCleanup
                .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

        TykClient client = new TykClient(channel);

        CoprocessMiniRequestObject.MiniRequestObject miniRequestObj = CoprocessMiniRequestObject.MiniRequestObject
                .newBuilder().build();

        CoprocessObject.Object req = CoprocessObject.Object.newBuilder().setHookName("MyPreMiddleware")
                .setRequest(miniRequestObj).build();

        // Send the request:
        CoprocessObject.Object modifiedReq = client.dispatchRequest(req);

        // Assert that the number of injected headers is ok:
        assertEquals(modifiedReq.getRequest().getSetHeadersCount(), 1);
    }

    @Test
    public void testAuthHook() throws Exception {
        GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

        DispatcherGrpc.DispatcherImplBase serviceImpl = mock(DispatcherGrpc.DispatcherImplBase.class,
        delegatesTo(new TykDispatcher()));
        
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful
        // shutdown.
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());

        // Create a client channel and register for automatic graceful shutdown.
        ManagedChannel channel = grpcCleanup
                .register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

        TykClient client = new TykClient(channel);

        // Test a bad authentication:
        CoprocessMiniRequestObject.MiniRequestObject requestObj = CoprocessMiniRequestObject.MiniRequestObject
                .newBuilder().build();

        CoprocessObject.Object req = CoprocessObject.Object.newBuilder().setHookName("MyAuthHook")
                .setRequest(requestObj).build();

        CoprocessObject.Object modifiedReq = client.dispatchRequest(req);

        assertEquals(modifiedReq.getRequest().getReturnOverrides().getResponseCode(), 403);

        
        // Now test a successful authentication:
        CoprocessMiniRequestObject.MiniRequestObject validReqObj = CoprocessMiniRequestObject.MiniRequestObject.newBuilder()
                .putHeaders("Authorization", "0cc984058c452f207f788efab86c1293")
                .build();

        CoprocessObject.Object validReq = CoprocessObject.Object.newBuilder()
                .setHookName("MyAuthHook")
                .setRequest(validReqObj).build();

        CoprocessObject.Object modifiedReq2 = client.dispatchRequest(validReq);

        // Check if the token was successfully injected in the metadata field:
        assertEquals(modifiedReq2.getMetadataOrDefault("token", ""), "0cc984058c452f207f788efab86c1293");
        
    }

}