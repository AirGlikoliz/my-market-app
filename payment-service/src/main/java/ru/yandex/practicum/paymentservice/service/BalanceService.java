package ru.yandex.practicum.paymentservice.service;

import reactor.core.publisher.Mono;

public interface BalanceService {

    Mono<Long> getBalance();

    Mono<PaymentResponse> pay(long amount);
}
