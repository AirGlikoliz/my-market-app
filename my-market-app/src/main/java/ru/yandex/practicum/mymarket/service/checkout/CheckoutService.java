package ru.yandex.practicum.mymarket.service.checkout;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CheckoutResult;

public interface CheckoutService {

    Mono<CheckoutResult> checkout(String username);
}
