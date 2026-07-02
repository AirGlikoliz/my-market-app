package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.entity.Order;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {

    @Query("SELECT * FROM orders ORDER BY order_date DESC")
    Flux<Order> findAllOrderByDateDesc();
}