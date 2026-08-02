package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.PaymentResult;
import ru.yandex.practicum.mymarket.dto.PaymentStatus;

public interface PaymentService {

    Mono<PaymentStatus> checkBalance(String username, Long requiredAmount);

    Mono<PaymentResult> pay(String username, Long amount);
}
