package ru.yandex.practicum.paymentservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.paymentservice.model.BalanceResponse;
import ru.yandex.practicum.paymentservice.model.PaymentRequest;
import ru.yandex.practicum.paymentservice.model.PaymentResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = "payment.balance.initial=50000")
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void getBalance_ShouldReturnCurrentBalance() {
        BalanceResponse response = webTestClient.get()
                .uri("/api/v1/payments/balance")
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
                .bodyValue(new PaymentRequest().amount(amount))
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
                .bodyValue(new PaymentRequest().amount(amount))
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
                .bodyValue(new PaymentRequest().amount(0L))
                .exchange()
                .expectStatus().isBadRequest();
    }

    private long currentBalance() {
        BalanceResponse response = webTestClient.get()
                .uri("/api/v1/payments/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BalanceResponse.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(response);
        return response.getBalance();
    }
}
