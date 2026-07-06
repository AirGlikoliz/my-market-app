package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;

import java.util.Map;

public interface CartService {

    Mono<Void> removeFromCart(String sessionId, Long itemId);

    Mono<Void> increaseQuantity(String sessionId, Long itemId);

    Mono<Void> decreaseQuantity(String sessionId, Long itemId);

    Mono<Void> clearCart(String sessionId);

    Mono<Map<Long, Integer>> getCart(String sessionId);

    Flux<ItemDto> getCartItems(String sessionId);

    Mono<Long> getTotalPrice(String sessionId);

    Mono<Void> applyAction(String sessionId, Long itemId, String action);
}