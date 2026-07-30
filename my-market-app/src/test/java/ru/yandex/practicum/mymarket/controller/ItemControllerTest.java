package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.config.WebConfig;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortOption;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(ItemController.class)
@Import(WebConfig.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient client;

    private WebTestClient webTestClient;

    @MockBean
    private ItemService itemService;

    @MockBean
    private CartService cartService;

    @BeforeEach
    void setUp() {
        webTestClient = client.mutateWith(mockUser("buyer1")).mutateWith(csrf());
    }

    @Test
    void getItems_ShouldReturnItemsPage() {
        ItemDto item1 = item(1L, "Мяч футбольный", "Профессиональный футбольный мяч", "/images/ball.jpg", 2500L);
        ItemDto item2 = item(2L, "Теннисная ракетка", "Облегченная теннисная ракетка", "/images/racket.jpg", 4500L);

        Page<ItemDto> itemPage = new PageImpl<>(List.of(item1, item2));

        when(itemService.getItems(anyString(), any(SortOption.class), anyInt(), anyInt())).thenReturn(Mono.just(itemPage));
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
                    assertNotNull(body);
                    assertTrue(body.contains("Мяч футбольный"));
                    assertTrue(body.contains("Теннисная ракетка"));
                    assertTrue(body.contains("items"));
                });

        verify(itemService, times(1)).getItems(anyString(), any(SortOption.class), anyInt(), anyInt());
        verify(cartService, times(1)).getCart(anyString());
    }

    @Test
    void getItems_WithSearchAndSort_ShouldReturnFilteredItems() {
        ItemDto item = item(1L, "Мяч футбольный", "Профессиональный футбольный мяч", "/images/ball.jpg", 2500L);
        Page<ItemDto> itemPage = new PageImpl<>(List.of(item));

        when(itemService.getItems("мяч", SortOption.ALPHA, 1, 5)).thenReturn(Mono.just(itemPage));
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
                    assertNotNull(body);
                    assertTrue(body.contains("Мяч футбольный"));
                });

        verify(itemService, times(1)).getItems("мяч", SortOption.ALPHA, 1, 5);
    }

    @Test
    void getItems_WithLowercaseSort_ShouldStillBind() {
        ItemDto item = item(1L, "Мяч футбольный", "Профессиональный футбольный мяч", "/images/ball.jpg", 2500L);
        Page<ItemDto> itemPage = new PageImpl<>(List.of(item));

        when(itemService.getItems(any(), eq(SortOption.PRICE), anyInt(), anyInt())).thenReturn(Mono.just(itemPage));
        when(cartService.getCart(anyString())).thenReturn(Mono.just(Map.of()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("sort", "price")
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(itemService, times(1)).getItems(any(), eq(SortOption.PRICE), anyInt(), anyInt());
    }

    @Test
    void getItems_WithUnknownSort_ShouldReturnBadRequest() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("sort", "UNKNOWN")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();

        verify(itemService, never()).getItems(anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void getItems_WithPaging_ShouldReturnPagingInfo() {
        ItemDto item = item(1L, "Мяч", null, null, 2500L);
        Page<ItemDto> itemPage = new PageImpl<>(List.of(item));

        when(itemService.getItems(anyString(), any(SortOption.class), eq(2), eq(2))).thenReturn(Mono.just(itemPage));
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
                    assertNotNull(body);
                    assertTrue(body.contains("Страница: 2"));
                });

        verify(itemService, times(1)).getItems(anyString(), any(SortOption.class), eq(2), eq(2));
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
                    assertNotNull(body);
                    assertTrue(body.contains("Мяч футбольный"));
                    assertTrue(body.contains("2500"));
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
                    assertNotNull(body);
                    assertTrue(body.contains("Мяч футбольный"));
                    assertTrue(body.contains("3"));
                });

        verify(itemService, times(1)).getItemById(itemId);
        verify(cartService, times(1)).getCart(anyString());
    }

    @Test
    void updateCartFromItems_WithPlusAction_ShouldApplyActionAndRedirect() {
        Long itemId = 1L;
        when(cartService.applyAction(anyString(), eq(itemId), eq(CartAction.PLUS))).thenReturn(Mono.empty());

        postItemsAction(itemId, "PLUS")
                .expectStatus().is3xxRedirection();

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq(CartAction.PLUS));
    }

    @Test
    void updateCartFromItems_WithMinusAction_ShouldApplyActionAndRedirect() {
        Long itemId = 2L;
        when(cartService.applyAction(anyString(), eq(itemId), eq(CartAction.MINUS))).thenReturn(Mono.empty());

        postItemsAction(itemId, "MINUS")
                .expectStatus().is3xxRedirection();

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq(CartAction.MINUS));
    }

    @Test
    void updateCartFromItems_WithUnknownAction_ShouldReturnBadRequest() {
        Long itemId = 1L;

        postItemsAction(itemId, "UNKNOWN")
                .expectStatus().isBadRequest();

        verify(cartService, never()).applyAction(anyString(), any(), any());
    }

    @Test
    void updateCartFromItems_WithSearchParams_ShouldRedirectWithSameParams() {
        Long itemId = 1L;
        when(cartService.applyAction(anyString(), eq(itemId), eq(CartAction.PLUS))).thenReturn(Mono.empty());

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

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq(CartAction.PLUS));
    }

    @Test
    void updateCartFromItems_WithMissingId_ShouldReturnBadRequest() {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("action", "PLUS")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "5")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();

        verify(cartService, never()).applyAction(anyString(), any(), any());
    }

    @Test
    void updateCartFromItems_WithPageSizeOverMax_ShouldReturnBadRequest() {
        Long itemId = 1L;

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId)
                        .queryParam("action", "PLUS")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "1000")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();

        verify(cartService, never()).applyAction(anyString(), any(), any());
    }

    @Test
    void updateCartFromItem_WithPlusAction_ShouldApplyActionAndRedirect() {
        Long itemId = 1L;
        when(cartService.applyAction(anyString(), eq(itemId), eq(CartAction.PLUS))).thenReturn(Mono.empty());

        postItemAction(itemId, "PLUS")
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq(CartAction.PLUS));
    }

    @Test
    void updateCartFromItem_WithMinusAction_ShouldApplyActionAndRedirect() {
        Long itemId = 2L;
        when(cartService.applyAction(anyString(), eq(itemId), eq(CartAction.MINUS))).thenReturn(Mono.empty());

        postItemAction(itemId, "MINUS")
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService, times(1)).applyAction(anyString(), eq(itemId), eq(CartAction.MINUS));
    }

    @Test
    void updateCartFromItem_WithUnknownAction_ShouldReturnBadRequest() {
        Long itemId = 1L;

        postItemAction(itemId, "UNKNOWN")
                .expectStatus().isBadRequest();

        verify(cartService, never()).applyAction(anyString(), any(), any());
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