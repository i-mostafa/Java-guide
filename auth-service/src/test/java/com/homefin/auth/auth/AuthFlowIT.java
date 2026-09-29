package com.homefin.auth.auth;

import com.homefin.auth.support.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// "import static" imports static METHODS so they can be called without the class name:
// post(...) instead of MockMvcRequestBuilders.post(...). Like import { post } from '...'.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test: HTTP -> security -> service -> Postgres (Testcontainers). Named *IT -> failsafe.
 *
 * <p>Maven runs *Test classes with the surefire plugin ("mvn test") and *IT classes with the
 * failsafe plugin ("mvn verify"), so slow integration tests are a separate phase.
 *
 * <p>Node analogy: a jest + supertest suite against the real Express app, with real Postgres and
 * Kafka started in Docker. MockMvc plays the role of supertest: it drives the full Spring MVC stack
 * (filters, security, controllers, error handler) in-process, without opening a network port.
 */
// @SpringBootTest (runtime): starts the complete application context for the test class, like
// bootstrapping the whole NestJS app in beforeAll. The context is cached and reused across test
// classes with the same configuration.
@SpringBootTest
// @AutoConfigureMockMvc: also create a MockMvc bean wired to that context.
@AutoConfigureMockMvc
// @ActiveProfiles("test"): activate the "test" profile -> application-test.yml is loaded.
@ActiveProfiles("test")
// @Import: add the Testcontainers bean definitions (Postgres + Kafka containers) to the context.
@Import(TestcontainersConfig.class)
// Package-private class: JUnit 5 doesn't need test classes or methods to be public.
class AuthFlowIT {

    // @Autowired (runtime): field injection - Spring sets this field from the context. Fine in tests;
    // in production code prefer constructor injection.
    @Autowired
    MockMvc mvc;

    // @Test (JUnit 5): marks a test case, like it('...') / test('...') in jest.
    // "throws Exception": perform(...) declares a checked exception; the test simply fails if thrown.
    @Test
    void registerThenLoginThenCallMe() throws Exception {
        // mvc.perform(request).andExpect(...) ~ supertest's request(app).post(url).send(body).expect(201).
        // The """ ... """ text block is a multi-line string literal (like a JS template literal
        // without interpolation); indentation common to all lines is stripped.
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"it.user@example.com","password":"S3cure#Passw0rd",
                         "firstName":"It","lastName":"User"}"""))
                .andExpect(status().isCreated())
                // jsonPath("$.role") selects a field in the JSON response body (JSONPath syntax, $ = root).
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        // andReturn() gives the raw result so we can read the body, like res.text in supertest.
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"it.user@example.com","password":"S3cure#Passw0rd"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        // Fully-qualified call to the JsonPath library (not imported) to extract the token string.
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("it.user@example.com"));
    }

    // Checks the GlobalExceptionHandler (common-lib) output for Bean Validation errors.
    @Test
    void registerRejectsInvalidPayloadWithProblemDetail() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"not-an-email","password":"short","firstName":"","lastName":"X"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("validation-failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"nobody@example.com","password":"whatever"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
