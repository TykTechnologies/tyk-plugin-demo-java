package com.tyktechnologies.tykmiddleware;

import coprocess.DispatcherGrpc;
import coprocess.CoprocessObject;

import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TykClient {
  private static final Logger logger = Logger.getLogger(TykClient.class.getName());

  private final ManagedChannel channel;
  private final DispatcherGrpc.DispatcherBlockingStub blockingStub;

  public TykClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext(true)
        .build());
  }

  TykClient(ManagedChannel channel) {
    this.channel = channel;
    blockingStub = DispatcherGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public CoprocessObject.Object dispatchRequest(CoprocessObject.Object req) {
    CoprocessObject.Object modifiedReq = null;
    try {
      modifiedReq = blockingStub.dispatch(req);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return modifiedReq;
    }
    return modifiedReq;
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Initializing gRPC client");
    TykClient client = new TykClient("localhost", 5555);

    CoprocessObject.Object req = CoprocessObject.Object.newBuilder()
    .setHookName("MyPreMiddleware")
    .build();

    logger.log(Level.INFO, "Dispatching request:" + req.toString());

    try {
      CoprocessObject.Object modifiedReq = client.dispatchRequest(req);
      logger.log(Level.INFO, "Got request:" + modifiedReq.toString());
    } finally {
      client.shutdown();
    }
  }
}
