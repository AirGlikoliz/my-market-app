package ru.yandex.practicum.paymentservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.paymentservice.model.BalanceResponse;
import ru.yandex.practicum.paymentservice.model.PaymentRequest;
import ru.yandex.practicum.paymentservice.model.PaymentResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "payment.balance.initial=50000",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9000/oauth2/jwks"
})
class PaymentControllerTest {

    private static final String USERNAME = "buyer1";

    @Autowired
    private WebTestClient client;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("SCOPE_payment.access")));
    }

    @Test
    void getBalance_WithoutToken_ShouldReturnUnauthorized() {
        client.get()
                .uri("/api/v1/payments/balance?username={username}", USERNAME)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void makePayment_WithoutToken_ShouldReturnUnauthorized() {
        client.post()
                .uri("/api/v1/payments")
                .bodyValue(new PaymentRequest().amount(100L).username(USERNAME))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBalance_ShouldReturnCurrentBalance() {
        BalanceResponse response = webTestClient.get()
                .uri("/api/v1/payments/balance?username={username}", USERNAME)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BalanceResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNotNull(response.getBalance());
        assertTrue(response.getBalance() >= 0);
    }

    @Test
    void makePayment_WithSufficientBalance_ShouldSucceedAndReduceBalance() {
        long currentBalance = currentBalance();
        long amount = Math.max(1, currentBalance / 2);

        PaymentResponse response = webTestClient.post()
                .uri("/api/v1/payments")
                .bodyValue(new PaymentRequest().amount(amount).username(USERNAME))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(currentBalance - amount, response.getBalance());
        assertEquals(currentBalance - amount, currentBalance());
    }

    @Test
    void makePayment_WithInsufficientBalance_ShouldFailAndKeepBalanceUnchanged() {
        long currentBalance = currentBalance();
        long amount = currentBalance + 1000000;

        PaymentResponse response = webTestClient.post()
                .uri("/api/v1/payments")
                .bodyValue(new PaymentRequest().amount(amount).username(USERNAME))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals(currentBalance, response.getBalance());
        assertEquals(currentBalance, currentBalance());
    }

    @Test
    void makePayment_WithNonPositiveAmount_ShouldReturnBadRequest() {
        webTestClient.post()
                .uri("/api/v1/payments")
                .bodyValue(new PaymentRequest().amount(0L).username(USERNAME))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void makePayment_ShouldNotAffectOtherUsersBalance() {
        String otherUsername = "buyer2";
        long otherBalanceBefore = balanceOf(otherUsername);

        webTestClient.post()
                .uri("/api/v1/payments")
                .bodyValue(new PaymentRequest().amount(1000L).username(USERNAME))
                .exchange()
                .expectStatus().isOk();

        assertEquals(otherBalanceBefore, balanceOf(otherUsername));
    }

    private long currentBalance() {
        return balanceOf(USERNAME);
    }

    private long balanceOf(String username) {
        BalanceResponse response = webTestClient.get()
                .uri("/api/v1/payments/balance?username={username}", username)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BalanceResponse.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(response);
        return response.getBalance();
    }
}
