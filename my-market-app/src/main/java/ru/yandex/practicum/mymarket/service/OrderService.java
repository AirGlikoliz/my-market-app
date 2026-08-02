package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartSnapshot;
import ru.yandex.practicum.mymarket.dto.OrderDto;

public interface OrderService {

    Mono<OrderDto> createPendingOrder(String username, CartSnapshot snapshot);

    Mono<Void> markPaid(Long orderId);

    Mono<Void> markFailed(Long orderId);

    Mono<OrderDto> getOrderById(Long id, String username);

    Flux<OrderDto> getAllOrders(String username);
}
