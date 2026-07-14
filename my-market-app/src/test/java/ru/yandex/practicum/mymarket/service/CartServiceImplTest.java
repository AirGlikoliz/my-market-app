package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cache.ItemCacheRepository;
import ru.yandex.practicum.mymarket.config.RedisConfig;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.CartItem;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.cache.EmbeddedRedisConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataR2dbcTest
@ActiveProfiles("test")
@Import({CartServiceImpl.class, ItemServiceImpl.class, ItemCacheRepository.class, RedisConfig.class,
        RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class, EmbeddedRedisConfiguration.class})
class CartServiceImplTest {

    private static final String SESSION_ID = "123";

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartServiceImpl cartService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        item1 = itemRepository.save(Item.builder()
                .title("Мяч")
                .description("Футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .build()).block();

        item2 = itemRepository.save(Item.builder()
                .title("Ракетка")
                .description("Теннисная ракетка")
                .imgPath("/images/racket.jpg")
                .price(4500L)
                .build()).block();
    }

    @Test
    void increaseQuantity_WhenItemNotInCart_ShouldCreateNewEntry() {
        StepVerifier.create(cartService.increaseQuantity(SESSION_ID, item1.getId()))
                .verifyComplete();

        CartItem saved = cartItemRepository.findBySessionIdAndItemId(SESSION_ID, item1.getId()).block();
        assertNotNull(saved);
        assertEquals(SESSION_ID, saved.getSessionId());
        assertEquals(item1.getId(), saved.getItemId());
        assertEquals(1, saved.getQuantity());
    }

    @Test
    void increaseQuantity_WhenItemAlreadyInCart_ShouldIncrementQuantity() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();

        StepVerifier.create(cartService.increaseQuantity(SESSION_ID, item1.getId()))
                .verifyComplete();

        CartItem saved = cartItemRepository.findBySessionIdAndItemId(SESSION_ID, item1.getId()).block();
        assertNotNull(saved);
        assertEquals(2, saved.getQuantity());
    }

    @Test
    void removeFromCart_ShouldDeleteEntry() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();

        StepVerifier.create(cartService.removeFromCart(SESSION_ID, item1.getId()))
                .verifyComplete();

        CartItem removed = cartItemRepository.findBySessionIdAndItemId(SESSION_ID, item1.getId()).block();
        assertNull(removed);
    }

    @Test
    void decreaseQuantity_WhenCountGreaterThanOne_ShouldDecreaseCount() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();

        StepVerifier.create(cartService.decreaseQuantity(SESSION_ID, item1.getId()))
                .verifyComplete();

        CartItem saved = cartItemRepository.findBySessionIdAndItemId(SESSION_ID, item1.getId()).block();
        assertNotNull(saved);
        assertEquals(1, saved.getQuantity());
    }

    @Test
    void decreaseQuantity_WhenCountIsOne_ShouldRemoveItem() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();

        StepVerifier.create(cartService.decreaseQuantity(SESSION_ID, item1.getId()))
                .verifyComplete();

        CartItem removed = cartItemRepository.findBySessionIdAndItemId(SESSION_ID, item1.getId()).block();
        assertNull(removed);
    }

    @Test
    void decreaseQuantity_WhenItemNotInCart_ShouldDoNothing() {
        StepVerifier.create(cartService.decreaseQuantity(SESSION_ID, 999L))
                .verifyComplete();

        List<CartItem> cart = cartItemRepository.findBySessionId(SESSION_ID).collectList().block();
        assertNotNull(cart);
        assertTrue(cart.isEmpty());
    }

    @Test
    void clearCart_ShouldRemoveAllItems() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();
        cartService.increaseQuantity(SESSION_ID, item2.getId()).block();

        StepVerifier.create(cartService.clearCart(SESSION_ID))
                .verifyComplete();

        List<CartItem> cart = cartItemRepository.findBySessionId(SESSION_ID).collectList().block();
        assertNotNull(cart);
        assertTrue(cart.isEmpty());
    }

    @Test
    void getCart_ShouldReturnMapOfItemIdToQuantity() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();
        cartService.increaseQuantity(SESSION_ID, item2.getId()).block();

        StepVerifier.create(cartService.getCart(SESSION_ID))
                .assertNext(cart -> {
                    assertEquals(2, cart.size());
                    assertEquals(2, cart.get(item1.getId()));
                    assertEquals(1, cart.get(item2.getId()));
                })
                .verifyComplete();
    }

    @Test
    void getCart_WhenEmpty_ShouldReturnEmptyMap() {
        StepVerifier.create(cartService.getCart(SESSION_ID))
                .assertNext(cart -> assertTrue(cart.isEmpty()))
                .verifyComplete();
    }

    @Test
    void getCartItems_ShouldReturnItemsWithCounts() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();
        cartService.increaseQuantity(SESSION_ID, item2.getId()).block();

        List<ItemDto> cartItems = cartService.getCartItems(SESSION_ID)
                .collectList()
                .block();

        assertNotNull(cartItems);
        assertEquals(2, cartItems.size());

        ItemDto itemDto1 = cartItems.stream()
                .filter(i -> i.id().equals(item1.getId()))
                .findFirst()
                .orElseThrow();

        ItemDto itemDto2 = cartItems.stream()
                .filter(i -> i.id().equals(item2.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals("Мяч", itemDto1.title());
        assertEquals(2, itemDto1.count());
        assertEquals(2500L, itemDto1.price());

        assertEquals("Ракетка", itemDto2.title());
        assertEquals(1, itemDto2.count());
        assertEquals(4500L, itemDto2.price());
    }

    @Test
    void getCartItems_WhenCartEmpty_ShouldReturnEmptyFlux() {
        StepVerifier.create(cartService.getCartItems(SESSION_ID))
                .verifyComplete();
    }

    @Test
    void getTotalPrice_ShouldCalculateCorrectly() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();
        cartService.increaseQuantity(SESSION_ID, item2.getId()).block();

        StepVerifier.create(cartService.getTotalPrice(SESSION_ID))
                .expectNext(9500L)
                .verifyComplete();
    }

    @Test
    void getTotalPrice_WhenCartEmpty_ShouldReturnZero() {
        StepVerifier.create(cartService.getTotalPrice(SESSION_ID))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void applyAction_WithPlus_ShouldIncreaseQuantity() {
        StepVerifier.create(cartService.applyAction(SESSION_ID, item1.getId(), "PLUS"))
                .verifyComplete();

        CartItem saved = cartItemRepository.findBySessionIdAndItemId(SESSION_ID, item1.getId()).block();
        assertNotNull(saved);
        assertEquals(1, saved.getQuantity());
    }

    @Test
    void applyAction_WithMinus_ShouldDecreaseQuantity() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();

        StepVerifier.create(cartService.applyAction(SESSION_ID, item1.getId(), "MINUS"))
                .verifyComplete();

        CartItem saved = cartItemRepository.findBySessionIdAndItemId(SESSION_ID, item1.getId()).block();
        assertNotNull(saved);
        assertEquals(1, saved.getQuantity());
    }

    @Test
    void applyAction_WithDelete_ShouldRemoveFromCart() {
        cartService.increaseQuantity(SESSION_ID, item1.getId()).block();

        StepVerifier.create(cartService.applyAction(SESSION_ID, item1.getId(), "DELETE"))
                .verifyComplete();

        CartItem removed = cartItemRepository.findBySessionIdAndItemId(SESSION_ID, item1.getId()).block();
        assertNull(removed);
    }

    @Test
    void applyAction_WithLowercaseAction_ShouldStillWork() {
        StepVerifier.create(cartService.applyAction(SESSION_ID, item1.getId(), "plus"))
                .verifyComplete();

        CartItem saved = cartItemRepository.findBySessionIdAndItemId(SESSION_ID, item1.getId()).block();
        assertNotNull(saved);
        assertEquals(1, saved.getQuantity());
    }

    @Test
    void applyAction_WithUnknownAction_ShouldDoNothing() {
        StepVerifier.create(cartService.applyAction(SESSION_ID, item1.getId(), "UNKNOWN"))
                .verifyComplete();

        List<CartItem> cart = cartItemRepository.findBySessionId(SESSION_ID).collectList().block();
        assertNotNull(cart);
        assertTrue(cart.isEmpty());
    }

    @Test
    void applyAction_WithNullAction_ShouldDoNothing() {
        StepVerifier.create(cartService.applyAction(SESSION_ID, item1.getId(), null))
                .verifyComplete();

        List<CartItem> cart = cartItemRepository.findBySessionId(SESSION_ID).collectList().block();
        assertNotNull(cart);
        assertTrue(cart.isEmpty());
    }
}