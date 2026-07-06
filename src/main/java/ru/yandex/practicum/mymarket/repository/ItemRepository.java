package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.Item;

import java.util.Collection;

@Repository
public interface ItemRepository extends ReactiveCrudRepository<Item, Long>{

    @Query("""
        SELECT *
        FROM items
        WHERE (:search IS NULL OR :search = '')
           OR LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY
            CASE WHEN :sort = 'TITLE_ASC' THEN title END ASC,
            CASE WHEN :sort = 'PRICE_ASC' THEN price END ASC,
            id ASC
        LIMIT :#{#pageable.pageSize}
        OFFSET :#{#pageable.offset}
    """)
    Flux<Item> findPage(String search, String sort, Pageable pageable);

    @Query("""
        SELECT COUNT(*)
        FROM items
        WHERE (:search IS NULL OR :search = '')
           OR LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Mono<Long> countBySearch(String search);

    Flux<Item> findAllByIdIn(Collection<Long> ids);
}