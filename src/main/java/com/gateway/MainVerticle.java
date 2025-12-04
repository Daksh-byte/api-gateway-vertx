package com.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("Starting API Gateway Application...");

        vertx.deployVerticle(new ApiGatewayVerticle())
                .onSuccess(id -> {
                    System.out.println("✓ ApiGatewayVerticle deployed with ID: " + id);
                    startPromise.complete();
                })
                .onFailure(err -> {
                    System.err.println("✗ Failed to deploy ApiGatewayVerticle: " + err.getMessage());
                    startPromise.fail(err);
                });
    }
}