package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.PaymentResult;
import ru.yandex.practicum.mymarket.dto.PaymentStatus;

public interface PaymentService {

    Mono<PaymentStatus> checkBalance(Long requiredAmount);

    Mono<PaymentResult> pay(Long amount);
}
