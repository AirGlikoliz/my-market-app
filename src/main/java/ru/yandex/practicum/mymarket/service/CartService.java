package ru.yandex.practicum.mymarket.service;

import ru.yandex.practicum.mymarket.dto.ItemDto;

import java.util.List;
import java.util.Map;

public interface CartService {

    void removeFromCart(Long itemId);

    void increaseQuantity(Long itemId);

    void decreaseQuantity(Long itemId);

    void clearCart();

    Map<Long, Integer> getCart();

    List<ItemDto> getCartItems();

    Long getTotalPrice();
}
