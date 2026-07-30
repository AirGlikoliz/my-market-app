package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CheckoutResult;
import ru.yandex.practicum.mymarket.service.checkout.CheckoutService;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient client;

    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        webTestClient = client.mutateWith(mockUser("buyer1")).mutateWith(csrf());
    }

    @Test
    void getOrders_ShouldReturnOrdersPage() {
        List<OrderDto> orders = List.of(
                order(1L, 5000L, LocalDateTime.now().minusDays(1),
                        List.of(orderItem(1L, "Мяч", 2500L, 2))),
                order(2L, 9500L, LocalDateTime.now(),
                        List.of(orderItem(1L, "Мяч", 2500L, 2),
                                orderItem(2L, "Ракетка", 4500L, 1)))
        );

        when(orderService.getAllOrders(anyString())).thenReturn(Flux.fromIterable(orders));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Заказ №1"));
                    assertTrue(body.contains("Заказ №2"));
                    assertTrue(body.contains("5000"));
                    assertTrue(body.contains("9500"));
                });

        verify(orderService, times(1)).getAllOrders(anyString());
    }

    @Test
    void getOrders_WhenNoOrders_ShouldReturnEmptyPage() {
        when(orderService.getAllOrders(anyString())).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Витрина магазина"));
                });

        verify(orderService, times(1)).getAllOrders(anyString());
    }

    @Test
    void getOrder_ShouldReturnOrderPage() {
        Long orderId = 1L;
        OrderDto order = order(orderId, 5000L, LocalDateTime.now(),
                List.of(orderItem(1L, "Мяч", 2500L, 2)));

        when(orderService.getOrderById(eq(orderId), anyString())).thenReturn(Mono.just(order));

        webTestClient.get()
                .uri("/orders/{id}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Мяч"));
                    assertTrue(body.contains("5000"));
                });

        verify(orderService, times(1)).getOrderById(eq(orderId), anyString());
    }

    @Test
    void getOrder_WithNewOrderFlag_ShouldReturnOrderPage() {
        Long orderId = 1L;
        OrderDto order = order(orderId, 5000L, LocalDateTime.now(),
                List.of(orderItem(1L, "Мяч", 2500L, 2)));

        when(orderService.getOrderById(eq(orderId), anyString())).thenReturn(Mono.just(order));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders/{id}")
                        .queryParam("newOrder", "true")
                        .build(orderId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Успешная покупка"));
                });

        verify(orderService, times(1)).getOrderById(eq(orderId), anyString());
    }

    @Test
    void createOrder_WithSuccessfulCheckout_ShouldRedirectToNewOrder() {
        when(checkoutService.checkout(anyString())).thenReturn(Mono.just(CheckoutResult.success(1L)));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/1?newOrder=true");

        verify(checkoutService, times(1)).checkout(anyString());
    }

    @Test
    void createOrder_WithEmptyCart_ShouldRedirectToCart() {
        when(checkoutService.checkout(anyString())).thenReturn(Mono.just(CheckoutResult.emptyCart()));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(checkoutService, times(1)).checkout(anyString());
    }

    @Test
    void createOrder_WithPaymentDeclined_ShouldRedirectToCart() {
        when(checkoutService.checkout(anyString())).thenReturn(Mono.just(CheckoutResult.paymentDeclined()));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(checkoutService, times(1)).checkout(anyString());
    }

    private static OrderItemDto orderItem(Long id, String title, Long price, int count) {
        return OrderItemDto.builder().id(id).title(title).price(price).count(count).build();
    }

    private static OrderDto order(Long id, Long totalSum, LocalDateTime date, List<OrderItemDto> items) {
        return OrderDto.builder().id(id).orderDate(date).totalSum(totalSum).items(items).build();
    }
}
