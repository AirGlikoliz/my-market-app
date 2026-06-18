package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private ItemService itemService;

    @InjectMocks
    private CartServiceImpl cartService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
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

        // when
        cartService.clearCart();

        // then
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

        when(itemService.getItemEntitiesByIds(Set.of(1L, 2L))).thenReturn(Map.of(1L, item1, 2L,item2));

        // when
        List<ItemDto> cartItems = cartService.getCartItems();

        // then
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
    void getCartItems_WhenCartEmpty_ShouldReturnEmptyList() {
        // when
        List<ItemDto> cartItems = cartService.getCartItems();

        // then
        assertTrue(cartItems.isEmpty());
        verify(itemService, never()).getItemEntityById(anyLong());
    }

    @Test
    void getTotalPrice_ShouldCalculateCorrectly() {
        // given
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(1L);
        cartService.increaseQuantity(2L);

        when(itemService.getItemEntitiesByIds(anySet())).thenReturn(Map.of(1L, item1, 2L, item2));

        // when
        List<ItemDto> cartItems = cartService.getCartItems();
        Long total = cartService.getTotalPrice(cartItems);

        // then
        assertEquals(9500L, total);
        verify(itemService, times(1)).getItemEntitiesByIds(Set.of(1L, 2L));
    }
}
