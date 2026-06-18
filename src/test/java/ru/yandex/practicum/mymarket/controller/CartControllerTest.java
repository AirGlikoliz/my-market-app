package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Test
    void getCartItems_ShouldReturnCartPage() throws Exception {
        // given
        List<ItemDto> cartItems = List.of(
                ItemDto.builder()
                        .id(1L)
                        .title("Мяч")
                        .price(2500L)
                        .count(2)
                        .build(),
                ItemDto.builder()
                        .id(2L)
                        .title("Ракетка")
                        .price(4500L)
                        .count(1)
                        .build()
        );
        Long total = 9500L;

        when(cartService.getCartItems()).thenReturn(cartItems);
        when(cartService.getTotalPrice(anyList())).thenReturn(total);

        // when & then
        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", cartItems))
                .andExpect(model().attribute("total", total));

        verify(cartService, times(1)).getCartItems();
        verify(cartService, times(1)).getTotalPrice(cartItems);
    }

    @Test
    void getCartItems_WhenCartEmpty_ShouldReturnEmptyPage() throws Exception {
        // given
        List<ItemDto> emptyList = List.of();
        when(cartService.getCartItems()).thenReturn(emptyList);
        when(cartService.getTotalPrice(anyList())).thenReturn(0L);

        // when & then
        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", List.of()))
                .andExpect(model().attribute("total", 0L));

        verify(cartService, times(1)).getCartItems();
        verify(cartService, times(1)).getTotalPrice(emptyList);
    }

    @Test
    void updateCartItems_WithPlusAction_ShouldIncreaseQuantity() throws Exception {
        // given
        Long itemId = 1L;
        String action = "PLUS";

        // when & then
        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService, times(1)).increaseQuantity(itemId);
        verify(cartService, never()).decreaseQuantity(anyLong());
        verify(cartService, never()).removeFromCart(anyLong());
    }

    @Test
    void updateCartItems_WithMinusAction_ShouldDecreaseQuantity() throws Exception {
        // given
        Long itemId = 2L;
        String action = "MINUS";

        // when & then
        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService, times(1)).decreaseQuantity(itemId);
        verify(cartService, never()).increaseQuantity(anyLong());
        verify(cartService, never()).removeFromCart(anyLong());
    }

    @Test
    void updateCartItems_WithDeleteAction_ShouldRemoveFromCart() throws Exception {
        // given
        Long itemId = 3L;
        String action = "DELETE";

        // when & then
        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService, times(1)).removeFromCart(itemId);
        verify(cartService, never()).increaseQuantity(anyLong());
        verify(cartService, never()).decreaseQuantity(anyLong());
    }

    @Test
    void updateCartItems_WithUnknownAction_ShouldDoNothing() throws Exception {
        // given
        Long itemId = 1L;
        String action = "UNKNOWN";

        // when & then
        mockMvc.perform(post("/cart/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService, never()).increaseQuantity(anyLong());
        verify(cartService, never()).decreaseQuantity(anyLong());
        verify(cartService, never()).removeFromCart(anyLong());
    }
}