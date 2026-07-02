package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@WebFluxTest(CartController.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CartService cartService;

    @Test
    void getCartItems_ShouldReturnCartPage1() {
        // given
        List<ItemDto> cartItems = List.of(
                ItemDto.builder().id(1L).title("Мяч").price(2500L).count(2).build(),
                ItemDto.builder().id(2L).title("Ракетка").price(4500L).count(1).build()
        );
        Long total = 9500L;

        when(cartService.getCartItems()).thenReturn(Flux.fromIterable(cartItems));
        when(cartService.getTotalPrice()).thenReturn(Mono.just(total));

        // when & then
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Мяч");
                    assert body.contains("Ракетка");
                    assert body.contains("9500");
                    assert body.contains("cart");
                });
    }

    @Test
    void getCartItems_WhenCartEmpty_ShouldReturnEmptyPage() {
        // given
        when(cartService.getCartItems()).thenReturn(Flux.empty());
        when(cartService.getTotalPrice()).thenReturn(Mono.just(0L));

        // when & then
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateCartItems_WithPlusAction_ShouldIncreaseQuantityAndRedirect() {
        Long itemId = 1L;

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", itemId)
                        .queryParam("action", "PLUS")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService, times(1)).increaseQuantity(itemId);
        verify(cartService, never()).decreaseQuantity(anyLong());
        verify(cartService, never()).removeFromCart(anyLong());
    }

    @Test
    void updateCartItems_WithMinusAction_ShouldDecreaseQuantityAndRedirect() {
        Long itemId = 2L;

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", itemId)
                        .queryParam("action", "MINUS")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService, times(1)).decreaseQuantity(itemId);
        verify(cartService, never()).increaseQuantity(anyLong());
        verify(cartService, never()).removeFromCart(anyLong());
    }

    @Test
    void updateCartItems_WithDeleteAction_ShouldRemoveFromCartAndRedirect() {
        Long itemId = 3L;

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", itemId)
                        .queryParam("action", "DELETE")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService, times(1)).removeFromCart(itemId);
        verify(cartService, never()).increaseQuantity(anyLong());
        verify(cartService, never()).decreaseQuantity(anyLong());
    }

    @Test
    void updateCartItems_WithUnknownAction_ShouldRedirect() {
        Long itemId = 1L;

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", itemId)
                        .queryParam("action", "UNKNOWN")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService, never()).increaseQuantity(anyLong());
        verify(cartService, never()).decreaseQuantity(anyLong());
        verify(cartService, never()).removeFromCart(anyLong());
    }
}