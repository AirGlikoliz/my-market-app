package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private ItemService itemService;

    private CartServiceImpl cartService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(itemService);

        item1 = Item.builder()
                .id(1L)
                .title("Мяч")
                .description("Футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .build();

        item2 = Item.builder()
                .id(2L)
                .title("Ракетка")
                .description("Теннисная ракетка")
                .imgPath("/images/racket.jpg")
                .price(4500L)
                .build();
    }

    @Test
    void addToCart_ShouldAddItem() {
        // when
        cartService.increaseQuantity(1L);

        // then
        Map<Long, Integer> cart = cartService.getCart();
        assertEquals(1, cart.size());
        assertEquals(1, cart.get(1L));
    }

    @Test
    void addToCart_ShouldIncreaseQuantity() {
        // given
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(1L);

        // when
        Map<Long, Integer> cart = cartService.getCart();

        // then
        assertEquals(1, cart.size());
        assertEquals(2, cart.get(1L));
    }

    @Test
    void removeFromCart_ShouldRemoveItem() {
        // given
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(2L);

        // when
        cartService.removeFromCart(1L);

        // then
        Map<Long, Integer> cart = cartService.getCart();
        assertEquals(1, cart.size());
        assertFalse(cart.containsKey(1L));
        assertTrue(cart.containsKey(2L));
    }

    @Test
    void removeFromCart_WhenItemNotExists_ShouldDoNothing() {
        // when
        cartService.removeFromCart(999L);

        // then
        Map<Long, Integer> cart = cartService.getCart();
        assertTrue(cart.isEmpty());
    }

    @Test
    void decreaseQuantity_ShouldDecreaseCount() {
        // given
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(1L);

        // when
        cartService.decreaseQuantity(1L);

        // then
        Map<Long, Integer> cart = cartService.getCart();
        assertEquals(1, cart.get(1L));
    }

    @Test
    void decreaseQuantity_WhenCountIsOne_ShouldRemoveItem() {
        // given
        cartService.increaseQuantity(1L);

        // when
        cartService.decreaseQuantity(1L);

        // then
        Map<Long, Integer> cart = cartService.getCart();
        assertTrue(cart.isEmpty());
    }

    @Test
    void clearCart_ShouldRemoveAllItems() {
        // given
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(2L);

        // when / then
        StepVerifier.create(cartService.clearCart())
                .verifyComplete();

        Map<Long, Integer> cart = cartService.getCart();
        assertTrue(cart.isEmpty());
    }

    @Test
    void getCart_ShouldReturnCopyNotOriginal() {
        // given
        cartService.increaseQuantity(1L);
        Map<Long, Integer> cartCopy = cartService.getCart();

        // when
        cartCopy.remove(1L);

        // then
        Map<Long, Integer> actualCart = cartService.getCart();
        assertEquals(1, actualCart.size());
        assertTrue(actualCart.containsKey(1L));
    }

    @Test
    void getCartItems_ShouldReturnItemsWithCounts() {
        // given
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(2L);

        when(itemService.getItemEntitiesByIds(Set.of(1L, 2L)))
                .thenReturn(Flux.just(item1, item2));

        // when
        List<ItemDto> cartItems = cartService.getCartItems()
                .collectList()
                .block();

        // then
        assertNotNull(cartItems);
        assertEquals(2, cartItems.size());

        ItemDto itemDto1 = cartItems.stream()
                .filter(i -> i.id().equals(1L))
                .findFirst()
                .orElseThrow();

        ItemDto itemDto2 = cartItems.stream()
                .filter(i -> i.id().equals(2L))
                .findFirst()
                .orElseThrow();

        assertEquals("Мяч", itemDto1.title());
        assertEquals(2, itemDto1.count());
        assertEquals(2500L, itemDto1.price());

        assertEquals("Ракетка", itemDto2.title());
        assertEquals(1, itemDto2.count());
        assertEquals(4500L, itemDto2.price());

        verify(itemService, times(1)).getItemEntitiesByIds(Set.of(1L, 2L));
    }

    @Test
    void getCartItems_WhenCartEmpty_ShouldReturnEmptyFlux() {
        // when / then
        StepVerifier.create(cartService.getCartItems())
                .verifyComplete();

        verify(itemService, never()).getItemEntitiesByIds(anySet());
    }

    @Test
    void getTotalPrice_ShouldCalculateCorrectly() {
        // given
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(2L);

        when(itemService.getItemEntitiesByIds(Set.of(1L, 2L)))
                .thenReturn(Flux.just(item1, item2));

        // when / then
        StepVerifier.create(cartService.getTotalPrice())
                .expectNext(9500L)
                .verifyComplete();

        verify(itemService, times(1)).getItemEntitiesByIds(Set.of(1L, 2L));
    }
}