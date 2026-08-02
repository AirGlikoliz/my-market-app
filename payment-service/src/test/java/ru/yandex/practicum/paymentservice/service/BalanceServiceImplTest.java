package ru.yandex.practicum.paymentservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceServiceImplTest {

    private static final long INITIAL_BALANCE = 10000;
    private static final String USERNAME = "buyer1";

    private BalanceServiceImpl balanceService;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceServiceImpl(INITIAL_BALANCE);
    }

    @Test
    void getBalance_ShouldReturnInitialBalance() {
        StepVerifier.create(balanceService.getBalance(USERNAME))
                .expectNext(INITIAL_BALANCE)
                .verifyComplete();
    }

    @Test
    void pay_WithSufficientBalance_ShouldSucceedAndReduceBalance() {
        StepVerifier.create(balanceService.pay(USERNAME, 4000))
                .assertNext(outcome -> {
                    assertTrue(outcome.success());
                    assertEquals(6000, outcome.balance());
                })
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance(USERNAME))
                .expectNext(6000L)
                .verifyComplete();
    }

    @Test
    void pay_WithInsufficientBalance_ShouldFailAndKeepBalanceUnchanged() {
        StepVerifier.create(balanceService.pay(USERNAME, 20000))
                .assertNext(outcome -> {
                    assertTrue(!outcome.success());
                    assertEquals(INITIAL_BALANCE, outcome.balance());
                })
                .verifyComplete();

        StepVerifier.create(balanceService.getBalance(USERNAME))
                .expectNext(INITIAL_BALANCE)
                .verifyComplete();
    }

    @Test
    void pay_WithZeroOrNegativeAmount_ShouldThrowIllegalArgumentException() {
        StepVerifier.create(balanceService.pay(USERNAME, 0))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(balanceService.pay(USERNAME, -100))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void pay_ExactBalance_ShouldSucceedAndZeroOutBalance() {
        StepVerifier.create(balanceService.pay(USERNAME, INITIAL_BALANCE))
                .assertNext(outcome -> {
                    assertTrue(outcome.success());
                    assertEquals(0, outcome.balance());
                })
                .verifyComplete();
    }

    @Test
    void pay_ConcurrentPayments_ShouldNeverOverdraftBalance() {
        int attempts = 50;
        long amountEach = 300;

        AtomicInteger successCount = new AtomicInteger();

        Flux.range(0, attempts)
                .flatMap(i -> balanceService.pay(USERNAME, amountEach), attempts)
                .doOnNext(outcome -> {
                    if (outcome.success()) {
                        successCount.incrementAndGet();
                    }
                })
                .blockLast(Duration.ofSeconds(5));

        long expectedSuccesses = INITIAL_BALANCE / amountEach;
        assertEquals(expectedSuccesses, successCount.get());

        long finalBalance = Mono.from(balanceService.getBalance(USERNAME)).block();
        assertTrue(finalBalance >= 0);
        assertEquals(INITIAL_BALANCE - expectedSuccesses * amountEach, finalBalance);
    }

    @Test
    void pay_ShouldNotAffectOtherUsersBalance() {
        String otherUsername = "buyer2";

        balanceService.pay(USERNAME, 4000).block();

        StepVerifier.create(balanceService.getBalance(otherUsername))
                .expectNext(INITIAL_BALANCE)
                .verifyComplete();
    }
}
