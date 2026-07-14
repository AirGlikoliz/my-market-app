package ru.yandex.practicum.mymarket.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CachedItemPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ItemCacheRepository {

    private static final String ITEM_KEY_PREFIX = "market:item:";
    private static final String PAGE_KEY_PREFIX = "market:items:page:";

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    @Value("${market.cache.ttl:120s}")
    private  Duration ttl;

    public Mono<ItemDto> getItem(Long id) {
        return redisTemplate.opsForValue().get(ITEM_KEY_PREFIX + id)
                .cast(ItemDto.class)
                .onErrorResume(ex -> {
                    log.error("Redis read failed for item {}: {}", id, ex.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<Boolean> putItem(ItemDto item) {
        return redisTemplate.opsForValue().set(ITEM_KEY_PREFIX + item.id(), item, ttl)
                .onErrorResume(ex -> {
                    log.error("Redis write failed for item {}: {}", item.id(), ex.getMessage());
                    return Mono.just(false);
                });
    }

    public Mono<CachedItemPage> getPage(String cacheKey) {
        return redisTemplate.opsForValue().get(cacheKey)
                .cast(CachedItemPage.class)
                .onErrorResume(ex -> {
                    log.error("Redis read failed for page {}: {}", cacheKey, ex.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<Boolean> putPage(String cacheKey, CachedItemPage page) {
        return redisTemplate.opsForValue().set(cacheKey, page, ttl)
                .onErrorResume(ex -> {
                    log.error("Redis write failed for page {}: {}", cacheKey, ex.getMessage());
                    return Mono.just(false);
                });
    }

    public Mono<Map<Long, ItemDto>> getItems(Set<Long> ids) {
        var keys = ids.stream().map(id -> ITEM_KEY_PREFIX + id).toList();

        return redisTemplate.opsForValue().multiGet(keys)
                .map(values -> values.stream()
                        .filter(ItemDto.class::isInstance)
                        .map(ItemDto.class::cast)
                        .collect(Collectors.toMap(ItemDto::id, Function.identity())))
                .onErrorResume(ex -> {
                    log.error("Redis multiGet failed for {} items: {}", ids.size(), ex.getMessage());
                    return Mono.just(Map.of());
                });
    }

    public String buildPageKey(String search, String sortKey, int pageNumber, int pageSize) {
        return PAGE_KEY_PREFIX + search.trim().toLowerCase() + ":" + sortKey + ":" + pageNumber + ":" + pageSize;
    }
}
