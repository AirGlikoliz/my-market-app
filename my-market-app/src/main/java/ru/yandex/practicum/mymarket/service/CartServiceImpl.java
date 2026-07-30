package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.CartItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final ItemService itemService;
    private final CartItemRepository cartItemRepository;

    @Override
    public Mono<Void> removeFromCart(String username, Long itemId) {
        log.info("Removing item {} from cart, user {}", itemId, username);
        return cartItemRepository.deleteByUsernameAndItemId(username, itemId);
    }

    @Override
    public Mono<Void> increaseQuantity(String username, Long itemId) {
        log.info("Increasing quantity of item {}, user {}", itemId, username);
        return cartItemRepository.findByUsernameAndItemId(username, itemId)
            .flatMap(existing -> {
                existing.setQuantity(existing.getQuantity() + 1);
                return cartItemRepository.save(existing);
            })
            .switchIfEmpty(Mono.defer(() -> itemService.getItemEntityById(itemId)
                .flatMap(item -> cartItemRepository.save(
                    CartItem.builder()
                        .username(username)
                        .itemId(itemId)
                        .quantity(1)
                        .build()
                ))))
            .then();
    }

    @Override
    public Mono<Void> decreaseQuantity(String username, Long itemId) {
        log.info("Decreasing quantity of item {}, user {}", itemId, username);
        return cartItemRepository.findByUsernameAndItemId(username, itemId)
            .flatMap(existing -> {
                if (existing.getQuantity() <= 1) {
                    return cartItemRepository.delete(existing);
                }
                existing.setQuantity(existing.getQuantity() - 1);
                return cartItemRepository.save(existing).then();
            });
    }

    @Override
    public Mono<Void> clearCart(String username) {
        log.info("Clearing cart, user {}", username);
        return cartItemRepository.deleteByUsername(username);
    }

    @Override
    public Mono<Map<Long, Integer>> getCart(String username) {
        return cartItemRepository.findByUsername(username)
                .collect(Collectors.toMap(CartItem::getItemId, CartItem::getQuantity));
    }

    @Override
    public Flux<ItemDto> getCartItems(String username) {
        return cartItemRepository.findByUsername(username)
            .collectMap(CartItem::getItemId, CartItem::getQuantity)
            .flatMapMany(cart -> {
                if (cart.isEmpty()) return Flux.empty();
                return itemService.getItemDtosByIds(cart.keySet())
                     .map(item -> ItemDto.builder()
                         .id(item.id())
                         .title(item.title())
                         .description(item.description())
                         .imgPath(item.imgPath())
                         .price(item.price())
                         .count(cart.get(item.id()))
                         .build());
            });
    }

    @Override
    public Mono<Long> getTotalPrice(String username) {
        return getCartItems(username)
                .map(item -> item.price() * item.count())
                .reduce(0L, Long::sum)
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Void> applyAction(String username, Long itemId, CartAction action) {
        if (action == null) {
            log.warn("No action given for item {}, user {}", itemId, username);
            return Mono.empty();
        }
        return switch (action) {
            case PLUS -> increaseQuantity(username, itemId);
            case MINUS -> decreaseQuantity(username, itemId);
            case DELETE -> removeFromCart(username, itemId);
        };
    }
}
