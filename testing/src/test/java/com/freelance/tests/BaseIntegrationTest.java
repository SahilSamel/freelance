package com.freelance.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for all integration tests.
 * <p>
 * Prerequisites before running:
 * 1. Start the server:  ./gradlew :Launcher:run  (or run Main directly from your IDE)
 * 2. Run tests:         ./gradlew :testing:integrationTest
 * <p>
 * The server runs on https://localhost:8080.
 * SSL certificate validation is relaxed so self-signed dev certs work fine.
 */
public abstract class BaseIntegrationTest {

    protected static final String BASE_URL = "https://localhost:8080";

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.baseURI = BASE_URL;

        String keystorePath = "D:\\programming\\java\\Freelance\\Launcher\\src\\main\\resources\\keystore\\keystore.jks";

        RestAssured.config = RestAssuredConfig.config()
                .sslConfig(SSLConfig.sslConfig()
                        .trustStore(keystorePath, "changeit")
                        .allowAllHostnames())
                .objectMapperConfig(
                        ObjectMapperConfig.objectMapperConfig()
                                .jackson2ObjectMapperFactory((cls, charset) -> new ObjectMapper())
                );
    }

    /**
     * Convenience: returns the value of JSON field {@code token} from a 200 login response.
     * Useful for test methods that need a valid token to call protected endpoints.
     */
    protected String loginAndGetToken(String username, String password) {
        return RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(new com.freelance.auth.dto.AuthLoginRequestDTO(username, password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    /**
     * Convenience: logs in as the pre-seeded superadmin user.
     */
    protected String loginAsSuperAdmin() {
        return loginAndGetToken("superadmin", "Welcome@1");
    }
}
