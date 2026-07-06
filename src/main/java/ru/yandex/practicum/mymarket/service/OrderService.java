package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.OrderDto;

public interface OrderService {

    Mono<OrderDto> createOrderFromCart(String sessionId);

    Mono<OrderDto> getOrderById(Long id);

    Flux<OrderDto> getAllOrders();
}