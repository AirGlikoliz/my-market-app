package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.CartItem;

import java.time.LocalDateTime;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {

    Flux<CartItem> findBySessionId(String sessionId);

    Mono<CartItem> findBySessionIdAndItemId(String sessionId, Long itemId);

    Mono<Void> deleteBySessionId(String sessionId);

    Mono<Void> deleteBySessionIdAndItemId(String sessionId, Long itemId);

    @Query("DELETE FROM cart_items " +
            "WHERE session_id IN ( SELECT session_id " +
            "FROM cart_items GROUP BY session_id HAVING MAX(updated_at) < :deadline)")
    Mono<Void> deleteAllOlderThan(LocalDateTime deadline);
}