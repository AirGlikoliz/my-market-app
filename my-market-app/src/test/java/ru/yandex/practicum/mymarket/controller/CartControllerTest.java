package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PaymentStatus;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.PaymentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(CartController.class)
class CartControllerTest {

    @Autowired
    private WebTestClient client;

    private WebTestClient webTestClient;

    @MockBean
    private CartService cartService;

    @MockBean
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        webTestClient = client.mutateWith(mockUser("buyer1")).mutateWith(csrf());
    }

    @Test
    void getCartItems_ShouldReturnCartPage1() {
        List<ItemDto> cartItems = List.of(
                ItemDto.builder().id(1L).title("Мяч").price(2500L).count(2).build(),
                ItemDto.builder().id(2L).title("Ракетка").price(4500L).count(1).build()
        );
        Long total = 9500L;

        when(cartService.getCartItems(anyString())).thenReturn(Flux.fromIterable(cartItems));
        when(cartService.getTotalPrice(anyString())).thenReturn(Mono.just(total));
        when(paymentService.checkBalance(anyString(), eq(total))).thenReturn(Mono.just(
                PaymentStatus.builder().available(true).sufficientFunds(true).balance(50000L).build()));

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
        when(cartService.getCartItems(anyString())).thenReturn(Flux.empty());
        when(cartService.getTotalPrice(anyString())).thenReturn(Mono.just(0L));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();

        verify(paymentService, never()).checkBalance(any(), any());
    }

    @Test
    void getCartItems_WhenBalanceInsufficient_ShouldShowMessageInsteadOfBuyButton() {
        List<ItemDto> cartItems = List.of(ItemDto.builder().id(1L).title("Мяч").price(2500L).count(2).build());
        Long total = 5000L;

        when(cartService.getCartItems(anyString())).thenReturn(Flux.fromIterable(cartItems));
        when(cartService.getTotalPrice(anyString())).thenReturn(Mono.just(total));
        when(paymentService.checkBalance(anyString(), eq(total))).thenReturn(Mono.just(
                PaymentStatus.builder().available(true).sufficientFunds(false).balance(1000L).build()));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Недостаточно средств");
                });
    }

    @Test
    void getCartItems_WhenPaymentServiceUnavailable_ShouldShowUnavailableMessage() {
        List<ItemDto> cartItems = List.of(ItemDto.builder().id(1L).title("Мяч").price(2500L).count(2).build());
        Long total = 5000L;

        when(cartService.getCartItems(anyString())).thenReturn(Flux.fromIterable(cartItems));
        when(cartService.getTotalPrice(anyString())).thenReturn(Mono.just(total));
        when(paymentService.checkBalance(anyString(), eq(total))).thenReturn(Mono.just(
                PaymentStatus.builder().available(false).sufficientFunds(false).message("Сервис платежей недоступен").build()));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Сервис платежей недоступен");
                });
    }

    @Test
    void updateCartItems_WithPlusAction_ShouldIncreaseQuantityAndRedirect() {
        Long itemId = 1L;
        when(cartService.increaseQuantity(anyString(), eq(itemId))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", itemId)
                        .queryParam("action", "PLUS")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService, times(1)).increaseQuantity(anyString(), eq(itemId));
        verify(cartService, never()).decreaseQuantity(anyString(), anyLong());
        verify(cartService, never()).removeFromCart(anyString(), anyLong());
    }

    @Test
    void updateCartItems_WithMinusAction_ShouldDecreaseQuantityAndRedirect() {
        Long itemId = 2L;
        when(cartService.decreaseQuantity(anyString(), eq(itemId))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", itemId)
                        .queryParam("action", "MINUS")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService, times(1)).decreaseQuantity(anyString(), eq(itemId));
        verify(cartService, never()).increaseQuantity(anyString(), anyLong());
        verify(cartService, never()).removeFromCart(anyString(), anyLong());
    }

    @Test
    void updateCartItems_WithDeleteAction_ShouldRemoveFromCartAndRedirect() {
        Long itemId = 3L;
        when(cartService.removeFromCart(anyString(), eq(itemId))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cart/items")
                        .queryParam("id", itemId)
                        .queryParam("action", "DELETE")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService, times(1)).removeFromCart(anyString(), eq(itemId));
        verify(cartService, never()).increaseQuantity(anyString(), anyLong());
        verify(cartService, never()).decreaseQuantity(anyString(), anyLong());
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

        verify(cartService, never()).increaseQuantity(anyString(), anyLong());
        verify(cartService, never()).decreaseQuantity(anyString(), anyLong());
        verify(cartService, never()).removeFromCart(anyString(), anyLong());
    }
}