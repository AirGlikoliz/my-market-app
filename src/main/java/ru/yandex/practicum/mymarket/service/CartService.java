package ru.yandex.practicum.mymarket.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;

import java.util.Map;

public interface CartService {

    void removeFromCart(Long itemId);

    void increaseQuantity(Long itemId);

    void decreaseQuantity(Long itemId);

    Mono<Void> clearCart();

    Map<Long, Integer> getCart();

    Flux<ItemDto> getCartItems();

    Mono<Long> getTotalPrice();
}