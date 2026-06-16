package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
//Чтобы несколько пользователей имели свою корзину, добавил сессионный жизненный цикл бина
@SessionScope
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final ItemService itemService;
    private final Map<Long, Integer> cart = new ConcurrentHashMap<>();

    @Override
    public void removeFromCart(Long itemId) {
        log.info("Removing item {} from cart", itemId);
        cart.remove(itemId);
    }

    @Override
    public void increaseQuantity(Long itemId) {
        log.info("Increasing quantity of item {}", itemId);
        cart.merge(itemId, 1, Integer::sum);
    }

    @Override
    public void decreaseQuantity(Long itemId) {
        log.info("Decreasing quantity of item {}", itemId);

        Integer currentCount = cart.get(itemId);

        if (currentCount == null) return;

        if (currentCount <= 1) {
            cart.remove(itemId);
        } else {
            cart.put(itemId, currentCount - 1);
        }
    }

    @Override
    public void clearCart() {
        log.info("Clearing cart");
        cart.clear();
    }

    @Override
    public Map<Long, Integer> getCart() {
        return new HashMap<>(cart);
    }

    @Override
    public List<ItemDto> getCartItems() {
        return cart.entrySet().stream()
            .map(entry -> {
                Item item = itemService.getItemEntityById(entry.getKey());
                return ItemDto.builder()
                    .id(item.getId())
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .imgPath(item.getImgPath())
                    .price(item.getPrice())
                    .count(entry.getValue())
                    .build();
            })
            .collect(Collectors.toList());
    }

    @Override
    public Long getTotalPrice() {
        return cart.entrySet().stream()
            .mapToLong(entry -> {
                Item item = itemService.getItemEntityById(entry.getKey());
                return item.getPrice() * entry.getValue();
            })
            .sum();
    }
}