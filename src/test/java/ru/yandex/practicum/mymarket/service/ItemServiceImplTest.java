package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(ItemServiceImpl.class)
class ItemServiceImplTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();

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


        itemRepository.saveAll(List.of(item1, item2, item3, item4, item5));
    }

    @Test
    void getItems_WithoutSearch_ShouldReturnAllItems() {
        // given
        int pageNumber = 1;
        int pageSize = 10;
        String sort = "NO";

        // when
        Page<ItemDto> result = itemService.getItems(null, sort, pageNumber, pageSize);

        // then
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
    }

    @Test
    void getItems_WithSearch_ShouldReturnFilteredItems() {
        // given
        String search = "мяч";
        int pageNumber = 1;
        int pageSize = 10;
        String sort = "NO";

        // when
        Page<ItemDto> result = itemService.getItems(search, sort, pageNumber, pageSize);

        // then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(
                i -> i.title().toLowerCase().contains("мяч") ||
                        i.description().toLowerCase().contains("мяч")
        ));
    }

    @Test
    void getItems_WithSearchNotFound_ShouldReturnEmptyPage() {
        // given
        String search = "несуществующий товар";
        int pageNumber = 1;
        int pageSize = 10;
        String sort = "NO";

        // when
        Page<ItemDto> result = itemService.getItems(search, sort, pageNumber, pageSize);

        // then
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void getItems_WithAlphaSort_ShouldReturnSortedByTitle() {
        // given
        String sort = "ALPHA";
        int pageNumber = 1;
        int pageSize = 10;

        // when
        Page<ItemDto> result = itemService.getItems(null, sort, pageNumber, pageSize);

        // then
        assertNotNull(result);
        List<ItemDto> items = result.getContent();
        assertEquals(5, items.size());

        assertTrue(items.get(0).title().compareTo(items.get(1).title()) < 0);
    }

    @Test
    void getItems_WithPriceSort_ShouldReturnSortedByPrice() {
        // given
        String sort = "PRICE";
        int pageNumber = 1;
        int pageSize = 10;

        // when
        Page<ItemDto> result = itemService.getItems(null, sort, pageNumber, pageSize);

        // then
        assertNotNull(result);
        List<ItemDto> items = result.getContent();
        assertEquals(5, items.size());

        for (int i = 0; i < items.size() - 1; i++) {
            assertTrue(items.get(i).price() <= items.get(i + 1).price());
        }
    }

    @Test
    void getItems_WithPagination_ShouldReturnCorrectPage() {
        // given
        int pageNumber = 2;
        int pageSize = 2;
        String sort = "NO";

        // when
        Page<ItemDto> result = itemService.getItems(null, sort, pageNumber, pageSize);

        // then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void getItemById_ExistingItem_ShouldReturnItem() {
        // given
        List<Item> allItems = itemRepository.findAll();
        Long itemId = allItems.get(0).getId();

        // when
        ItemDto result = itemService.getItemById(itemId);

        // then
        assertNotNull(result);
        assertEquals(itemId, result.id());
        assertNotNull(result.title());
        assertNotNull(result.description());
        assertNotNull(result.imgPath());
        assertNotNull(result.price());
    }

    @Test
    void getItemById_NonExistingItem_ShouldThrowException() {
        // given
        Long nonExistingId = 999L;

        // when & then
        ItemNotFoundException exception = assertThrows(ItemNotFoundException.class,
                () -> itemService.getItemById(nonExistingId));

        assertEquals("Item not found with id: 999", exception.getMessage());
    }

    @Test
    void getItemEntityById_ExistingItem_ShouldReturnItemEntity() {
        // given
        List<Item> allItems = itemRepository.findAll();
        Long itemId = allItems.get(0).getId();

        // when
        Item result = itemService.getItemEntityById(itemId);

        // then
        assertNotNull(result);
        assertEquals(itemId, result.getId());
    }

    @Test
    void getItemEntityById_NonExistingItem_ShouldThrowException() {
        // given
        Long nonExistingId = 999L;

        // when & then
        ItemNotFoundException exception = assertThrows(ItemNotFoundException.class,
                () -> itemService.getItemEntityById(nonExistingId));

        assertEquals("Item not found with id: 999", exception.getMessage());
    }

    @Test
    void getItems_WithEmptySearch_ShouldReturnAllItems() {
        // given
        String search = "";
        String sort = "NO";
        int pageNumber = 1;
        int pageSize = 10;

        // when
        Page<ItemDto> result = itemService.getItems(search, sort, pageNumber, pageSize);

        // then
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
    }

    @Test
    void getItemEntitiesByIds_ShouldReturnAllItems() {
        // given
        List<Item> allItems = itemRepository.findAll();
        Set<Long> ids = Set.of(allItems.get(0).getId(), allItems.get(1).getId(), allItems.get(2).getId());

        // when
        Map<Long, Item> result = itemService.getItemEntitiesByIds(ids);

        // then
        assertNotNull(result);
        assertEquals(3, result.size());

        for (Long id : ids) {
            assertTrue(result.containsKey(id));
            assertNotNull(result.get(id));
        }
    }

    @Test
    void getItemEntitiesByIds_WithEmptySet_ShouldReturnEmptyMap() {
        // when
        Map<Long, Item> result = itemService.getItemEntitiesByIds(Set.of());

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}