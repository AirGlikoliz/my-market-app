package ru.yandex.practicum.paymentservice.service;

import reactor.core.publisher.Mono;

public interface BalanceService {

    Mono<Long> getBalance(String username);

    Mono<PaymentResponse> pay(String username, long amount);
}
