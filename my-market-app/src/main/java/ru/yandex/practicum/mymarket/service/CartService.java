package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;

import java.util.Map;

public interface CartService {

    Mono<Void> removeFromCart(String username, Long itemId);

    Mono<Void> increaseQuantity(String username, Long itemId);

    Mono<Void> decreaseQuantity(String username, Long itemId);

    Mono<Void> clearCart(String username);

    Mono<Map<Long, Integer>> getCart(String username);

    Flux<ItemDto> getCartItems(String username);

    Mono<Long> getTotalPrice(String username);

    Mono<Void> applyAction(String username, Long itemId, String action);
}
