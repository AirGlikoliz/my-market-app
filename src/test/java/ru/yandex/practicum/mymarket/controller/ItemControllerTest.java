package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PagingInfo;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @MockBean
    private CartService cartService;

    @Test
    void getItems_ShouldReturnItemsPage() throws Exception {
        // given
        ItemDto item1 = ItemDto.builder()
                .id(1L)
                .title("Мяч футбольный")
                .description("Профессиональный футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .count(0)
                .build();

        ItemDto item2 = ItemDto.builder()
                .id(2L)
                .title("Теннисная ракетка")
                .description("Облегченная теннисная ракетка")
                .imgPath("/images/racket.jpg")
                .price(4500L)
                .count(0)
                .build();

        Page<ItemDto> itemPage = new PageImpl<>(List.of(item1, item2));

        when(itemService.getItems(anyString(), anyString(), anyInt(), anyInt())).thenReturn(itemPage);
        when(cartService.getCart()).thenReturn(Map.of());

        // when & then
        mockMvc.perform(get("/items")
                        .param("search", "")
                        .param("sort", "NO")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("search"))
                .andExpect(model().attributeExists("sort"))
                .andExpect(model().attributeExists("paging"))
                .andExpect(model().attributeExists("pageSizes"));

        verify(itemService, times(1)).getItems(anyString(), anyString(), anyInt(), anyInt());
        verify(cartService, times(1)).getCart();
    }

    @Test
    void getItems_WithSearchAndSort_ShouldReturnFilteredItems() throws Exception {
        // given
        ItemDto item = ItemDto.builder()
                .id(1L)
                .title("Мяч футбольный")
                .description("Профессиональный футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .count(0)
                .build();

        Page<ItemDto> itemPage = new PageImpl<>(List.of(item));

        when(itemService.getItems(eq("мяч"), eq("ALPHA"), eq(1), eq(5))).thenReturn(itemPage);
        when(cartService.getCart()).thenReturn(Map.of());

        // when & then
        mockMvc.perform(get("/items")
                        .param("search", "мяч")
                        .param("sort", "ALPHA")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"));

        verify(itemService, times(1)).getItems("мяч", "ALPHA", 1, 5);
    }

    @Test
    void getItems_WithPaging_ShouldReturnPagingInfo() throws Exception {
        // given
        ItemDto item = ItemDto.builder()
                .id(1L)
                .title("Мяч")
                .price(2500L)
                .count(0)
                .build();

        Page<ItemDto> itemPage = new PageImpl<>(List.of(item));

        when(itemService.getItems(anyString(), anyString(), eq(2), eq(2))).thenReturn(itemPage);
        when(cartService.getCart()).thenReturn(Map.of());

        // when & then
        mockMvc.perform(get("/items")
                        .param("search", "")
                        .param("sort", "NO")
                        .param("pageNumber", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("paging", PagingInfo.builder()
                        .pageSize(2)
                        .pageNumber(2)
                        .hasPrevious(true)
                        .hasNext(false)
                        .build()));

        verify(itemService, times(1)).getItems(anyString(), anyString(), eq(2), eq(2));
    }

    @Test
    void getItem_ShouldReturnItemPage() throws Exception {
        // given
        Long itemId = 1L;
        ItemDto item = ItemDto.builder()
                .id(itemId)
                .title("Мяч футбольный")
                .description("Профессиональный футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .count(0)
                .build();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(cartService.getCart()).thenReturn(Map.of());

        // when & then
        mockMvc.perform(get("/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", item));

        verify(itemService, times(1)).getItemById(itemId);
        verify(cartService, times(1)).getCart();
    }

    @Test
    void getItem_WithItemInCart_ShouldReturnItemWithCount() throws Exception {
        // given
        Long itemId = 1L;
        ItemDto item = ItemDto.builder()
                .id(itemId)
                .title("Мяч футбольный")
                .price(2500L)
                .count(0)
                .build();

        ItemDto itemWithCount = ItemDto.builder()
                .id(itemId)
                .title("Мяч футбольный")
                .price(2500L)
                .count(3)
                .build();

        when(itemService.getItemById(itemId)).thenReturn(item);
        when(cartService.getCart()).thenReturn(Map.of(itemId, 3));

        // when & then
        mockMvc.perform(get("/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attribute("item", itemWithCount));

        verify(itemService, times(1)).getItemById(itemId);
        verify(cartService, times(1)).getCart();
    }

    @Test
    void updateCartFromItems_WithPlusAction_ShouldIncreaseQuantity() throws Exception {
        // given
        Long itemId = 1L;
        String action = "PLUS";

        // when & then
        mockMvc.perform(post("/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action))
                .andExpect(status().is3xxRedirection());

        verify(cartService, times(1)).increaseQuantity(itemId);
        verify(cartService, never()).decreaseQuantity(anyLong());
    }

    @Test
    void updateCartFromItems_WithMinusAction_ShouldDecreaseQuantity() throws Exception {
        // given
        Long itemId = 2L;
        String action = "MINUS";

        // when & then
        mockMvc.perform(post("/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action))
                .andExpect(status().is3xxRedirection());

        verify(cartService, times(1)).decreaseQuantity(itemId);
        verify(cartService, never()).increaseQuantity(anyLong());
    }

    @Test
    void updateCartFromItems_WithUnknownAction_ShouldDoNothing() throws Exception {
        // given
        Long itemId = 1L;
        String action = "UNKNOWN";

        // when & then
        mockMvc.perform(post("/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action))
                .andExpect(status().is3xxRedirection());

        verify(cartService, never()).increaseQuantity(anyLong());
        verify(cartService, never()).decreaseQuantity(anyLong());
    }

    @Test
    void updateCartFromItems_WithSearchParams_ShouldRedirectWithSameParams() throws Exception {
        // given
        Long itemId = 1L;
        String action = "PLUS";
        String search = "мяч";
        String sort = "ALPHA";
        int pageNumber = 2;
        int pageSize = 10;

        // when & then
        mockMvc.perform(post("/items")
                        .param("id", String.valueOf(itemId))
                        .param("action", action)
                        .param("search", search)
                        .param("sort", sort)
                        .param("pageNumber", String.valueOf(pageNumber))
                        .param("pageSize", String.valueOf(pageSize)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=мяч&sort=ALPHA&pageNumber=2&pageSize=10"));

        verify(cartService, times(1)).increaseQuantity(itemId);
    }

    @Test
    void updateCartFromItem_WithPlusAction_ShouldIncreaseQuantity() throws Exception {
        // given
        Long itemId = 1L;
        String action = "PLUS";

        // when & then
        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", action))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemId));

        verify(cartService, times(1)).increaseQuantity(itemId);
        verify(cartService, never()).decreaseQuantity(anyLong());
    }

    @Test
    void updateCartFromItem_WithMinusAction_ShouldDecreaseQuantity() throws Exception {
        // given
        Long itemId = 2L;
        String action = "MINUS";

        // when & then
        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", action))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemId));

        verify(cartService, times(1)).decreaseQuantity(itemId);
        verify(cartService, never()).increaseQuantity(anyLong());
    }

    @Test
    void updateCartFromItem_WithUnknownAction_ShouldDoNothing() throws Exception {
        // given
        Long itemId = 1L;
        String action = "UNKNOWN";

        // when & then
        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", action))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemId));

        verify(cartService, never()).increaseQuantity(anyLong());
        verify(cartService, never()).decreaseQuantity(anyLong());
    }
}
