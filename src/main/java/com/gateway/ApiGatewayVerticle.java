package com.gateway;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;  // Add this import
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class ApiGatewayVerticle extends AbstractVerticle {

    private WebClient webClient;
    private CircuitBreaker circuitBreaker;

    // ADD THIS MAIN METHOD
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ApiGatewayVerticle())
                .onSuccess(id -> System.out.println("✓ Deployment successful: " + id))
                .onFailure(err -> System.err.println("✗ Deployment failed: " + err.getMessage()));
    }

    @Override
    public void start(Promise<Void> startPromise) {
        // ... rest of your code stays the same
        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout(5000)
                .setIdleTimeout(10000);
        webClient = WebClient.create(vertx, options);

        CircuitBreakerOptions breakerOptions = new CircuitBreakerOptions()
                .setMaxFailures(3)
                .setTimeout(5000)
                .setResetTimeout(10000);
        circuitBreaker = CircuitBreaker.create("api-circuit-breaker", vertx, breakerOptions);

        Router router = Router.router(vertx);

        router.get("/aggregate").handler(this::handleAggregate);
        router.get("/health").handler(this::handleHealth);

        router.route().handler(ctx -> {
            ctx.response()
                    .setStatusCode(404)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Route not found").encode());
        });

        HttpServer server = vertx.createHttpServer();

        server.requestHandler(router)
                .listen(8080)
                .onSuccess(http -> {
                    System.out.println("✓ API Gateway started successfully on http://localhost:8080");
                    System.out.println("✓ Available endpoints:");
                    System.out.println("  - GET http://localhost:8080/aggregate");
                    System.out.println("  - GET http://localhost:8080/health");
                    startPromise.complete();
                })
                .onFailure(err -> {
                    System.err.println("✗ Failed to start server: " + err.getMessage());
                    startPromise.fail(err);
                });
    }

    private void handleAggregate(RoutingContext ctx) {
        System.out.println("→ Received request at /aggregate");

        Future<String> postTitleFuture = fetchPostTitle();
        Future<String> authorNameFuture = fetchAuthorName();

        CompositeFuture.all(postTitleFuture, authorNameFuture)
                .onSuccess(result -> {
                    String postTitle = postTitleFuture.result();
                    String authorName = authorNameFuture.result();

                    System.out.println("✓ Successfully fetched data:");
                    System.out.println("  - Post Title: " + postTitle);
                    System.out.println("  - Author Name: " + authorName);

                    JsonObject response = new JsonObject()
                            .put("post_title", postTitle)
                            .put("author_name", authorName);

                    ctx.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(response.encodePrettily());
                })
                .onFailure(err -> {
                    System.err.println("✗ Error fetching data: " + err.getMessage());

                    JsonObject errorResponse = new JsonObject()
                            .put("error", "Failed to fetch data from external APIs")
                            .put("message", err.getMessage())
                            .put("timestamp", System.currentTimeMillis());

                    ctx.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(500)
                            .end(errorResponse.encodePrettily());
                });
    }

    private void handleHealth(RoutingContext ctx) {
        JsonObject health = new JsonObject()
                .put("status", "UP")
                .put("timestamp", System.currentTimeMillis())
                .put("service", "API Gateway");

        ctx.response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end(health.encodePrettily());
    }

    private Future<String> fetchPostTitle() {
        Promise<String> promise = Promise.promise();

        System.out.println("  → Fetching post data...");

        circuitBreaker.execute(cbPromise -> {
            webClient
                    .get(443, "jsonplaceholder.typicode.com", "/posts/1")
                    .ssl(true)
                    .send()
                    .onSuccess(response -> {
                        if (response.statusCode() == 200) {
                            JsonObject body = response.bodyAsJsonObject();
                            String title = body.getString("title", "Unknown Title");
                            System.out.println("  ✓ Post fetched successfully");
                            cbPromise.complete(title);
                        } else {
                            String error = "Post API returned status: " + response.statusCode();
                            System.err.println("  ✗ " + error);
                            cbPromise.fail(error);
                        }
                    })
                    .onFailure(err -> {
                        System.err.println("  ✗ Failed to fetch post: " + err.getMessage());
                        cbPromise.fail(err);
                    });
        }).onComplete(ar -> {
            if (ar.succeeded()) {
                promise.complete((String) ar.result());
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    private Future<String> fetchAuthorName() {
        Promise<String> promise = Promise.promise();

        System.out.println("  → Fetching user data...");

        circuitBreaker.execute(cbPromise -> {
            webClient
                    .get(443, "jsonplaceholder.typicode.com", "/users/1")
                    .ssl(true)
                    .send()
                    .onSuccess(response -> {
                        if (response.statusCode() == 200) {
                            JsonObject body = response.bodyAsJsonObject();
                            String name = body.getString("name", "Unknown Author");
                            System.out.println("  ✓ User fetched successfully");
                            cbPromise.complete(name);
                        } else {
                            String error = "User API returned status: " + response.statusCode();
                            System.err.println("  ✗ " + error);
                            cbPromise.fail(error);
                        }
                    })
                    .onFailure(err -> {
                        System.err.println("  ✗ Failed to fetch user: " + err.getMessage());
                        cbPromise.fail(err);
                    });
        }).onComplete(ar -> {
            if (ar.succeeded()) {
                promise.complete((String) ar.result());
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        System.out.println("Shutting down API Gateway...");
        if (webClient != null) {
            webClient.close();
        }
        if (circuitBreaker != null) {
            circuitBreaker.close();
        }
        stopPromise.complete();
    }
}