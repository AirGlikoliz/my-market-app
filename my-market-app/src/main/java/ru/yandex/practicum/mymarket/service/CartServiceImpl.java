package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
    public Mono<Void> removeFromCart(String sessionId, Long itemId) {
        log.info("Removing item {} from cart, session {}", itemId, sessionId);
        return cartItemRepository.deleteBySessionIdAndItemId(sessionId, itemId);
    }

    @Override
    public Mono<Void> increaseQuantity(String sessionId, Long itemId) {
        log.info("Increasing quantity of item {}, session {}", itemId, sessionId);
        return cartItemRepository.findBySessionIdAndItemId(sessionId, itemId)
            .flatMap(existing -> {
                existing.setQuantity(existing.getQuantity() + 1);
                return cartItemRepository.save(existing);
            })
            .switchIfEmpty(Mono.defer(() -> cartItemRepository.save(
                CartItem.builder()
                    .sessionId(sessionId)
                    .itemId(itemId)
                    .quantity(1)
                    .build()
            )))
            .then();
    }

    @Override
    public Mono<Void> decreaseQuantity(String sessionId, Long itemId) {
        log.info("Decreasing quantity of item {}, session {}", itemId, sessionId);
        return cartItemRepository.findBySessionIdAndItemId(sessionId, itemId)
            .flatMap(existing -> {
                if (existing.getQuantity() <= 1) {
                    return cartItemRepository.delete(existing);
                }
                existing.setQuantity(existing.getQuantity() - 1);
                return cartItemRepository.save(existing).then();
            });
    }

    @Override
    public Mono<Void> clearCart(String sessionId) {
        log.info("Clearing cart, session {}", sessionId);
        return cartItemRepository.deleteBySessionId(sessionId);
    }

    @Override
    public Mono<Map<Long, Integer>> getCart(String sessionId) {
        return cartItemRepository.findBySessionId(sessionId)
                .collect(Collectors.toMap(CartItem::getItemId, CartItem::getQuantity));
    }

    @Override
    public Flux<ItemDto> getCartItems(String sessionId) {
        return cartItemRepository.findBySessionId(sessionId)
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
    public Mono<Long> getTotalPrice(String sessionId) {
        return getCartItems(sessionId)
                .map(item -> item.price() * item.count())
                .reduce(0L, Long::sum)
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Void> applyAction(String sessionId, Long itemId, String action) {
        String normalizedAction = action != null ? action.toUpperCase() : "";

        return switch (normalizedAction) {
            case "PLUS" -> increaseQuantity(sessionId, itemId);
            case "MINUS" -> decreaseQuantity(sessionId, itemId);
            case "DELETE" -> removeFromCart(sessionId, itemId);
            default -> {
                log.warn("Unknown action: {}", action);
                yield Mono.empty();
            }
        };
    }
}