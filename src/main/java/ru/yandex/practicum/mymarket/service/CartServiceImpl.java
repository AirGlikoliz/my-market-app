package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
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
    public Mono<Void> clearCart() {
        log.info("Clearing cart");
        cart.clear();
        return Mono.empty();
    }

    @Override
    public Map<Long, Integer> getCart() {
        return new HashMap<>(cart);
    }

    @Override
    public Flux<ItemDto> getCartItems() {
        if (cart.isEmpty()) return Flux.empty();
        return itemService.getItemEntitiesByIds(cart.keySet())
            .map(item -> {
                Integer count = cart.get(item.getId());
                return ItemDto.builder()
                    .id(item.getId())
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .imgPath(item.getImgPath())
                    .price(item.getPrice())
                    .count(count)
                    .build();
            });
    }

    @Override
    public Mono<Long> getTotalPrice() {
        if (cart.isEmpty()) return Mono.just(0L);
        return getCartItems()
                .map(item -> item.price() * item.count())
                .reduce(0L, Long::sum);
    }
}