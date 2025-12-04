package com.gateway;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiGatewayTest {

    private static Vertx vertx;
    private static WebClient client;

    @BeforeAll
    static void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        client = WebClient.create(vertx);

        System.out.println("Deploying ApiGatewayVerticle for testing...");

        vertx.deployVerticle(new ApiGatewayVerticle())
                .onComplete(testContext.succeedingThenComplete());
    }

    @AfterAll
    static void tearDown(VertxTestContext testContext) {
        System.out.println("Closing Vert.x instance...");
        if (client != null) {
            client.close();
        }
        if (vertx != null) {
            vertx.close().onComplete(testContext.succeedingThenComplete());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test /health endpoint returns UP status")
    void testHealthEndpoint(VertxTestContext testContext) throws Throwable {
        client.get(8080, "localhost", "/health")
                .send()
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(200, response.statusCode());

                    JsonObject body = response.bodyAsJsonObject();
                    assertEquals("UP", body.getString("status"));

                    System.out.println("✓ Health check passed");
                    testContext.completeNow();
                })));

        assertTrue(testContext.awaitCompletion(10, TimeUnit.SECONDS));
    }

    @Test
    @Order(2)
    @DisplayName("Test /aggregate endpoint returns correct structure")
    void testAggregateEndpoint(VertxTestContext testContext) throws Throwable {
        client.get(8080, "localhost", "/aggregate")
                .send()
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    assertEquals("application/json", response.getHeader("content-type"));

                    JsonObject body = response.bodyAsJsonObject();
                    assertNotNull(body);

                    // Verify structure
                    assertTrue(body.containsKey("post_title"));
                    assertTrue(body.containsKey("author_name"));

                    // Verify values are not null or empty
                    assertNotNull(body.getString("post_title"));
                    assertNotNull(body.getString("author_name"));
                    assertFalse(body.getString("post_title").isEmpty());
                    assertFalse(body.getString("author_name").isEmpty());

                    System.out.println("✓ Aggregate endpoint test passed");
                    System.out.println("Response: " + body.encodePrettily());

                    testContext.completeNow();
                })));

        assertTrue(testContext.awaitCompletion(15, TimeUnit.SECONDS));
    }

    @Test
    @Order(3)
    @DisplayName("Test /aggregate endpoint response values")
    void testAggregateResponseValues(VertxTestContext testContext) throws Throwable {
        client.get(8080, "localhost", "/aggregate")
                .send()
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    JsonObject body = response.bodyAsJsonObject();

                    String postTitle = body.getString("post_title");
                    String authorName = body.getString("author_name");

                    // These values come from JSONPlaceholder
                    assertNotEquals("Unknown Title", postTitle);
                    assertNotEquals("Unknown Author", authorName);

                    System.out.println("✓ Response values validated");
                    System.out.println("  Post Title: " + postTitle);
                    System.out.println("  Author Name: " + authorName);

                    testContext.completeNow();
                })));

        assertTrue(testContext.awaitCompletion(15, TimeUnit.SECONDS));
    }

    @Test
    @Order(4)
    @DisplayName("Test 404 for undefined route")
    void testUndefinedRoute(VertxTestContext testContext) throws Throwable {
        client.get(8080, "localhost", "/undefined")
                .send()
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(404, response.statusCode());

                    JsonObject body = response.bodyAsJsonObject();
                    assertTrue(body.containsKey("error"));

                    System.out.println("✓ 404 handling test passed");
                    testContext.completeNow();
                })));

        assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    }
}