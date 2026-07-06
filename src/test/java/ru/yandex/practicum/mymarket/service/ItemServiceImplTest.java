package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataR2dbcTest
@ActiveProfiles("test")
@Import(ItemServiceImpl.class)
class ItemServiceImplTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemServiceImpl itemService;

    private List<Item> savedItems;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll().block();

        Item item1 = Item.builder()
                .title("Мяч футбольный")
                .description("Профессиональный футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .build();

        Item item2 = Item.builder()
                .title("Мяч футбольный 2")
                .description("Любительский футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .build();

        Item item3 = Item.builder()
                .title("Теннисная ракетка")
                .description("Облегченная теннисная ракетка")
                .imgPath("/images/racket.jpg")
                .price(4500L)
                .build();

        Item item4 = Item.builder()
                .title("Беговые кроссовки")
                .description("Удобные беговые кроссовки")
                .imgPath("/images/sneakers.jpg")
                .price(8900L)
                .build();

        Item item5 = Item.builder()
                .title("Футболка спортивная")
                .description("Дышащая спортивная футболка")
                .imgPath("/images/tshirt.jpg")
                .price(1200L)
                .build();

        savedItems = itemRepository.saveAll(List.of(item1, item2, item3, item4, item5))
                .collectList()
                .block();
    }

    @Test
    void getItems_WithoutSearch_ShouldReturnAllItems() {
        int pageNumber = 1;
        int pageSize = 10;
        String sort = "NO";

        Page<ItemDto> result = itemService.getItems(null, sort, pageNumber, pageSize).block();

        assertNotNull(result);
        assertEquals(5, result.getContent().size());
    }

    @Test
    void getItems_WithSearch_ShouldReturnFilteredItems() {
        String search = "мяч";
        int pageNumber = 1;
        int pageSize = 10;
        String sort = "NO";

        Page<ItemDto> result = itemService.getItems(search, sort, pageNumber, pageSize).block();

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(
                i -> i.title().toLowerCase().contains("мяч") ||
                        i.description().toLowerCase().contains("мяч")
        ));
    }

    @Test
    void getItems_WithSearchNotFound_ShouldReturnEmptyPage() {
        String search = "несуществующий товар";
        int pageNumber = 1;
        int pageSize = 10;
        String sort = "NO";

        Page<ItemDto> result = itemService.getItems(search, sort, pageNumber, pageSize).block();

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void getItems_WithAlphaSort_ShouldReturnSortedByTitle() {
        String sort = "ALPHA";
        int pageNumber = 1;
        int pageSize = 10;

        Page<ItemDto> result = itemService.getItems(null, sort, pageNumber, pageSize).block();

        assertNotNull(result);
        List<ItemDto> items = result.getContent();
        assertEquals(5, items.size());

        assertTrue(items.get(0).title().compareTo(items.get(1).title()) < 0);
    }

    @Test
    void getItems_WithPriceSort_ShouldReturnSortedByPrice() {
        String sort = "PRICE";
        int pageNumber = 1;
        int pageSize = 10;

        Page<ItemDto> result = itemService.getItems(null, sort, pageNumber, pageSize).block();

        assertNotNull(result);
        List<ItemDto> items = result.getContent();
        assertEquals(5, items.size());

        for (int i = 0; i < items.size() - 1; i++) {
            assertTrue(items.get(i).price() <= items.get(i + 1).price());
        }
    }

    @Test
    void getItems_WithPagination_ShouldReturnCorrectPage() {
        int pageNumber = 2;
        int pageSize = 2;
        String sort = "NO";

        Page<ItemDto> result = itemService.getItems(null, sort, pageNumber, pageSize).block();

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void getItemById_ExistingItem_ShouldReturnItem() {
        Long itemId = savedItems.get(0).getId();

        StepVerifier.create(itemService.getItemById(itemId))
                .assertNext(result -> {
                    assertEquals(itemId, result.id());
                    assertNotNull(result.title());
                    assertNotNull(result.description());
                    assertNotNull(result.imgPath());
                    assertNotNull(result.price());
                })
                .verifyComplete();
    }

    @Test
    void getItemById_NonExistingItem_ShouldThrowException() {
        Long nonExistingId = 999L;

        StepVerifier.create(itemService.getItemById(nonExistingId))
                .expectErrorMatches(ex -> ex instanceof ItemNotFoundException
                        && ex.getMessage().equals("Item not found with id: 999"))
                .verify();
    }

    @Test
    void getItemEntityById_ExistingItem_ShouldReturnItemEntity() {
        Long itemId = savedItems.get(0).getId();

        StepVerifier.create(itemService.getItemEntityById(itemId))
                .assertNext(result -> assertEquals(itemId, result.getId()))
                .verifyComplete();
    }

    @Test
    void getItemEntityById_NonExistingItem_ShouldThrowException() {
        Long nonExistingId = 999L;

        StepVerifier.create(itemService.getItemEntityById(nonExistingId))
                .expectErrorMatches(ex -> ex instanceof ItemNotFoundException
                        && ex.getMessage().equals("Item not found with id: 999"))
                .verify();
    }

    @Test
    void getItems_WithEmptySearch_ShouldReturnAllItems() {
        String search = "";
        String sort = "NO";
        int pageNumber = 1;
        int pageSize = 10;

        Page<ItemDto> result = itemService.getItems(search, sort, pageNumber, pageSize).block();

        assertNotNull(result);
        assertEquals(5, result.getContent().size());
    }

    @Test
    void getItemEntitiesByIds_ShouldReturnAllItems() {
        Set<Long> ids = Set.of(
                savedItems.get(0).getId(),
                savedItems.get(1).getId(),
                savedItems.get(2).getId()
        );

        List<Item> result = itemService.getItemEntitiesByIds(ids)
                .collectList()
                .block();

        assertNotNull(result);
        assertEquals(3, result.size());

        List<Long> resultIds = result.stream().map(Item::getId).toList();
        for (Long id : ids) {
            assertTrue(resultIds.contains(id));
        }
    }

    @Test
    void getItemEntitiesByIds_WithEmptySet_ShouldReturnEmptyFlux() {
        StepVerifier.create(itemService.getItemEntitiesByIds(Set.of()))
                .verifyComplete();
    }
}