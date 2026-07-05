package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebFluxTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ItemService itemService;

    @MockBean
    private CartService cartService;

    @Test
    void getItems_ShouldReturnItemsPage() {
        ItemDto item1 = item(1L, "Мяч футбольный", "Профессиональный футбольный мяч", "/images/ball.jpg", 2500L);
        ItemDto item2 = item(2L, "Теннисная ракетка", "Облегченная теннисная ракетка", "/images/racket.jpg", 4500L);

        Page<ItemDto> itemPage = new PageImpl<>(List.of(item1, item2));

        when(itemService.getItems(anyString(), anyString(), anyInt(), anyInt())).thenReturn(Mono.just(itemPage));
        when(cartService.getCart(anyString())).thenReturn(Mono.just(Map.of()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "5")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Мяч футбольный");
                    assert body.contains("Теннисная ракетка");
                    assert body.contains("items");
                });

        verify(itemService, times(1)).getItems(anyString(), anyString(), anyInt(), anyInt());
        verify(cartService, times(1)).getCart(anyString());
    }

    @Test
    void getItems_WithSearchAndSort_ShouldReturnFilteredItems() {
        ItemDto item = item(1L, "Мяч футбольный", "Профессиональный футбольный мяч", "/images/ball.jpg", 2500L);
        Page<ItemDto> itemPage = new PageImpl<>(List.of(item));

        when(itemService.getItems("мяч", "ALPHA", 1, 5)).thenReturn(Mono.just(itemPage));
        when(cartService.getCart(anyString())).thenReturn(Mono.just(Map.of()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "мяч")
                        .queryParam("sort", "ALPHA")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "5")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Мяч футбольный");
                });

        verify(itemService, times(1)).getItems("мяч", "ALPHA", 1, 5);
    }

    @Test
    void getItems_WithPaging_ShouldReturnPagingInfo() {
        ItemDto item = item(1L, "Мяч", null, null, 2500L);
        Page<ItemDto> itemPage = new PageImpl<>(List.of(item));

        when(itemService.getItems(anyString(), anyString(), eq(2), eq(2))).thenReturn(Mono.just(itemPage));
        when(cartService.getCart(anyString())).thenReturn(Mono.just(Map.of()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageNumber", "2")
                        .queryParam("pageSize", "2")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Страница: 2");
                });

        verify(itemService, times(1)).getItems(anyString(), anyString(), eq(2), eq(2));
    }

    @Test
    void getItem_ShouldReturnItemPage() {
        Long itemId = 1L;
        ItemDto item = item(itemId, "Мяч футбольный", "Профессиональный футбольный мяч", "/images/ball.jpg", 2500L);

        when(itemService.getItemById(itemId)).thenReturn(Mono.just(item));
        when(cartService.getCart(anyString())).thenReturn(Mono.just(Map.of()));

        webTestClient.get()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Мяч футбольный");
                    assert body.contains("2500");
                });

        verify(itemService, times(1)).getItemById(itemId);
        verify(cartService, times(1)).getCart(anyString());
    }

    @Test
    void getItem_WithItemInCart_ShouldReturnItemWithCount() {
        Long itemId = 1L;
        ItemDto item = item(itemId, "Мяч футбольный", null, null, 2500L);

        when(itemService.getItemById(itemId)).thenReturn(Mono.just(item));
        when(cartService.getCart(anyString())).thenReturn(Mono.just(Map.of(itemId, 3)));

        webTestClient.get()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Мяч футбольный");
                    assert body.contains("3");
                });

        verify(itemService, times(1)).getItemById(itemId);
        verify(cartService, times(1)).getCart(anyString());
    }

    @Test
    void updateCartFromItems_WithPlusAction_ShouldApplyActionAndRedirect() {
        Long itemId = 1L;
        when(cartService.applyAction(anyString(), eq(itemId), eq("PLUS"))).thenReturn(Mono.empty());

        postItemsAction(itemId, "PLUS")
                .expectStatus().is3xxRedirection();

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq("PLUS"));
    }

    @Test
    void updateCartFromItems_WithMinusAction_ShouldApplyActionAndRedirect() {
        Long itemId = 2L;
        when(cartService.applyAction(anyString(), eq(itemId), eq("MINUS"))).thenReturn(Mono.empty());

        postItemsAction(itemId, "MINUS")
                .expectStatus().is3xxRedirection();

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq("MINUS"));
    }

    @Test
    void updateCartFromItems_WithUnknownAction_ShouldRedirect() {
        Long itemId = 1L;
        when(cartService.applyAction(anyString(), eq(itemId), eq("UNKNOWN"))).thenReturn(Mono.empty());

        postItemsAction(itemId, "UNKNOWN")
                .expectStatus().is3xxRedirection();

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq("UNKNOWN"));
    }

    @Test
    void updateCartFromItems_WithSearchParams_ShouldRedirectWithSameParams() {
        Long itemId = 1L;
        when(cartService.applyAction(anyString(), eq(itemId), eq("PLUS"))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId)
                        .queryParam("action", "PLUS")
                        .queryParam("search", "мяч")
                        .queryParam("sort", "ALPHA")
                        .queryParam("pageNumber", "2")
                        .queryParam("pageSize", "10")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items?search=%D0%BC%D1%8F%D1%87&sort=ALPHA&pageNumber=2&pageSize=10");

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq("PLUS"));
    }

    @Test
    void updateCartFromItem_WithPlusAction_ShouldApplyActionAndRedirect() {
        Long itemId = 1L;
        when(cartService.applyAction(anyString(), eq(itemId), eq("PLUS"))).thenReturn(Mono.empty());

        postItemAction(itemId, "PLUS")
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq("PLUS"));
    }

    @Test
    void updateCartFromItem_WithMinusAction_ShouldApplyActionAndRedirect() {
        Long itemId = 2L;
        when(cartService.applyAction(anyString(), eq(itemId), eq("MINUS"))).thenReturn(Mono.empty());

        postItemAction(itemId, "MINUS")
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq("MINUS"));
    }

    @Test
    void updateCartFromItem_WithUnknownAction_ShouldRedirect() {
        Long itemId = 1L;
        when(cartService.applyAction(anyString(), eq(itemId), eq("UNKNOWN"))).thenReturn(Mono.empty());

        postItemAction(itemId, "UNKNOWN")
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq("UNKNOWN"));
    }

    private static ItemDto item(Long id, String title, String description, String imgPath, Long price) {
        return ItemDto.builder()
                .id(id)
                .title(title)
                .description(description)
                .imgPath(imgPath)
                .price(price)
                .count(0)
                .build();
    }

    private WebTestClient.ResponseSpec postItemsAction(Long itemId, String action) {
        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId)
                        .queryParam("action", action)
                        .build())
                .exchange();
    }

    private WebTestClient.ResponseSpec postItemAction(Long itemId, String action) {
        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", action)
                        .build(itemId))
                .exchange();
    }
}