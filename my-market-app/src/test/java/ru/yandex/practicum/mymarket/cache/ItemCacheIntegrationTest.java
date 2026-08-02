package ru.yandex.practicum.mymarket.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.config.RedisConfig;
import ru.yandex.practicum.mymarket.dto.CachedItemPage;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.ItemServiceImpl;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataR2dbcTest
@ActiveProfiles("test")
@TestPropertySource(properties = "market.cache.ttl=2s")
@Import({ItemServiceImpl.class, ItemCacheRepository.class, RedisConfig.class,
        RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class, EmbeddedRedisConfiguration.class})
class ItemCacheIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemServiceImpl itemService;

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    private Item savedItem;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll().block();
        redisTemplate.keys("market:*").flatMap(redisTemplate::delete).blockLast();

        savedItem = itemRepository.save(Item.builder()
                .title("Мяч футбольный")
                .description("Профессиональный футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .build()).block();
    }

    @Test
    void getItemById_ShouldPopulateRedisCacheOnFirstCall() {
        StepVerifier.create(itemService.getItemById(savedItem.getId()))
                .expectNextCount(1)
                .verifyComplete();

        Object cached = redisTemplate.opsForValue().get("market:item:" + savedItem.getId()).block();
        assertNotNull(cached);
        assertTrue(cached instanceof ItemDto);
        assertEquals(savedItem.getId(), ((ItemDto) cached).id());
    }

    @Test
    void getItemById_ShouldBeServedFromCache_AfterItemRemovedFromDb() {
        itemService.getItemById(savedItem.getId()).block();

        itemRepository.deleteById(savedItem.getId()).block();

        StepVerifier.create(itemService.getItemById(savedItem.getId()))
                .assertNext(dto -> assertEquals(savedItem.getId(), dto.id()))
                .verifyComplete();
    }

    @Test
    void getItemById_ShouldFallBackToDb_AfterCacheEntryExpires() {
        itemService.getItemById(savedItem.getId()).block();
        itemRepository.deleteById(savedItem.getId()).block();

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> StepVerifier.create(itemService.getItemById(savedItem.getId()))
                .expectErrorMatches(ex -> ex instanceof ItemNotFoundException)
                .verify());
    }

    @Test
    void getItems_ShouldPopulateRedisCacheOnFirstCall() {
        StepVerifier.create(itemService.getItems(null, SortOption.NO, 1, 10))
                .expectNextCount(1)
                .verifyComplete();

        Object cached = redisTemplate.opsForValue().get("market:items:page::NONE:1:10").block();
        assertNotNull(cached);
        assertTrue(cached instanceof CachedItemPage);
        assertEquals(1, ((CachedItemPage) cached).totalElements());
    }

    @Test
    void getItems_ShouldBeServedFromCache_AfterItemsRemovedFromDb() {
        itemService.getItems(null, SortOption.NO, 1, 10).block();

        itemRepository.deleteAll().block();

        StepVerifier.create(itemService.getItems(null, SortOption.NO, 1, 10))
                .assertNext((Page<ItemDto> page) -> assertEquals(1, page.getContent().size()))
                .verifyComplete();
    }

    @Test
    void getItemDtosByIds_ShouldPopulateRedisCacheOnFirstCall() {
        StepVerifier.create(itemService.getItemDtosByIds(Set.of(savedItem.getId())))
                .expectNextCount(1)
                .verifyComplete();

        Object cached = redisTemplate.opsForValue().get("market:item:" + savedItem.getId()).block();
        assertNotNull(cached);
        assertTrue(cached instanceof ItemDto);
    }

    @Test
    void getItemDtosByIds_ShouldBeServedFromCache_AfterItemRemovedFromDb() {
        itemService.getItemDtosByIds(Set.of(savedItem.getId())).blockLast();

        itemRepository.deleteById(savedItem.getId()).block();

        StepVerifier.create(itemService.getItemDtosByIds(Set.of(savedItem.getId())))
                .assertNext(dto -> assertEquals(savedItem.getId(), dto.id()))
                .verifyComplete();
    }

    @Test
    void getItemDtosByIds_ShouldMergeCachedItemWithDbOnlyItemInSingleBatch() {
        Item dbOnlyItem = itemRepository.save(Item.builder()
                .title("Теннисная ракетка")
                .description("Облегченная теннисная ракетка")
                .imgPath("/images/racket.jpg")
                .price(4500L)
                .build()).block();

        itemService.getItemDtosByIds(Set.of(savedItem.getId())).blockLast();
        itemRepository.deleteById(savedItem.getId()).block();

        List<ItemDto> result = itemService.getItemDtosByIds(Set.of(savedItem.getId(), dbOnlyItem.getId()))
                .collectList()
                .block();

        assertNotNull(result);
        assertEquals(2, result.size());

        List<Long> resultIds = result.stream().map(ItemDto::id).toList();
        assertTrue(resultIds.contains(savedItem.getId()));
        assertTrue(resultIds.contains(dbOnlyItem.getId()));
    }
}
