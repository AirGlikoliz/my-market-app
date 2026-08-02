package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.CartItem;

import java.time.LocalDateTime;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Flux<CartItem> findByUsername(String username);

    Mono<CartItem> findByUsernameAndItemId(String username, Long itemId);

    Mono<Void> deleteByUsername(String username);

    Mono<Void> deleteByUsernameAndItemId(String username, Long itemId);

    @Query("DELETE FROM cart_items " +
            "WHERE username IN ( SELECT username " +
            "FROM cart_items GROUP BY username HAVING MAX(updated_at) < :deadline)")
    Mono<Void> deleteAllOlderThan(LocalDateTime deadline);
}
