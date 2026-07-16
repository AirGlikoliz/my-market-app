package ru.yandex.practicum.mymarket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.config.PaymentClientConfig;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceImplTest {

    private static final long STUB_BALANCE = 10000;
    private static DisposableServer stubServer;
    private PaymentServiceImpl paymentService;

    @BeforeAll
    static void startStubServer() {
        stubServer = HttpServer.create()
            .port(0)
            .route(routes -> routes
                .get("/api/v1/payments/balance", (request, response) ->
                    response.header("Content-Type", "application/json")
                        .sendString(Mono.just("{\"balance\": " + STUB_BALANCE + "}")))
                .post("/api/v1/payments", (request, response) ->
                    request.receive().aggregate().asString()
                        .map(PaymentServiceImplTest::extractAmount)
                        .flatMap(amount -> response.header("Content-Type", "application/json")
                            .sendString(Mono.just(paymentResponseFor(amount)))
                            .then())))
            .bindNow();
    }

    private static long extractAmount(String requestBody) {
        try {
            JsonNode node = new ObjectMapper().readTree(requestBody);
            return node.get("amount").asLong();
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse request body: " + requestBody, e);
        }
    }

    private static String paymentResponseFor(long amount) {
        return amount <= STUB_BALANCE
                ? "{\"success\": true, \"balance\": " + (STUB_BALANCE - amount) + ", \"message\": \"Payment successful\"}"
                : "{\"success\": false, \"balance\": " + STUB_BALANCE + ", \"message\": \"Insufficient funds\"}";
    }

    @AfterAll
    static void stopStubServer() {
        stubServer.disposeNow();
    }

    @BeforeEach
    void setUp() {
        paymentService = paymentServiceFor("http://localhost:" + stubServer.port(), Duration.ofSeconds(2));
    }

    private static PaymentServiceImpl paymentServiceFor(String baseUrl, Duration timeout) {
        PaymentClientConfig config = new PaymentClientConfig();
        var apiClient = config.paymentApiClient(baseUrl, timeout);
        return new PaymentServiceImpl(config.paymentsApi(apiClient));
    }

    @Test
    void checkBalance_WithSufficientBalance_ShouldReturnAvailableAndSufficient() {
        StepVerifier.create(paymentService.checkBalance(5_000L))
                .assertNext(status -> {
                    assertTrue(status.available());
                    assertTrue(status.sufficientFunds());
                    assertTrue(status.checkoutAllowed());
                    assertEquals(STUB_BALANCE, status.balance());
                })
                .verifyComplete();
    }

    @Test
    void checkBalance_WithAmountAboveBalance_ShouldReturnInsufficientFunds() {
        StepVerifier.create(paymentService.checkBalance(STUB_BALANCE + 1))
                .assertNext(status -> {
                    assertTrue(status.available());
                    assertFalse(status.sufficientFunds());
                    assertFalse(status.checkoutAllowed());
                })
                .verifyComplete();
    }

    @Test
    void pay_WithAmountWithinBalance_ShouldReturnSuccess() {
        StepVerifier.create(paymentService.pay(5_000L))
                .assertNext(result -> {
                    assertTrue(result.success());
                    assertEquals("Payment successful", result.message());
                })
                .verifyComplete();
    }

    @Test
    void pay_WithAmountAboveBalance_ShouldReturnDeclined() {
        StepVerifier.create(paymentService.pay(STUB_BALANCE + 1))
                .assertNext(result -> {
                    assertFalse(result.success());
                    assertEquals("Insufficient funds", result.message());
                })
                .verifyComplete();
    }

    @Test
    void checkBalance_WhenEndpointUnreachable_ShouldReturnUnavailable() {
        PaymentServiceImpl unreachableService = paymentServiceFor("http://localhost:1", Duration.ofMillis(500));

        StepVerifier.create(unreachableService.checkBalance(1L))
                .assertNext(status -> {
                    assertFalse(status.available());
                    assertFalse(status.checkoutAllowed());
                    assertNotNull(status.message());
                })
                .verifyComplete();
    }

    @Test
    void pay_WhenEndpointUnreachable_ShouldReturnFailureResult() {
        PaymentServiceImpl unreachableService = paymentServiceFor("http://localhost:1", Duration.ofMillis(500));

        StepVerifier.create(unreachableService.pay(1L))
                .assertNext(result -> {
                    assertFalse(result.success());
                    assertNotNull(result.message());
                })
                .verifyComplete();
    }
}
