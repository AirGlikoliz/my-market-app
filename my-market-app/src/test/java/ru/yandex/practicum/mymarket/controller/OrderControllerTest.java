package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.dto.PaymentResult;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@WebFluxTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CartService cartService;

    @MockBean
    private PaymentService paymentService;

    @Test
    void getOrders_ShouldReturnOrdersPage() {
        List<OrderDto> orders = List.of(
                order(1L, 5000L, LocalDateTime.now().minusDays(1),
                        List.of(orderItem(1L, "Мяч", 2500L, 2))),
                order(2L, 9500L, LocalDateTime.now(),
                        List.of(orderItem(1L, "Мяч", 2500L, 2),
                                orderItem(2L, "Ракетка", 4500L, 1)))
        );

        when(orderService.getAllOrders()).thenReturn(Flux.fromIterable(orders));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Заказ №1");
                    assert body.contains("Заказ №2");
                    assert body.contains("5000");
                    assert body.contains("9500");
                });

        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void getOrders_WhenNoOrders_ShouldReturnEmptyPage() {
        when(orderService.getAllOrders()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Витрина магазина");
                });

        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void getOrder_ShouldReturnOrderPage() {
        Long orderId = 1L;
        OrderDto order = order(orderId, 5000L, LocalDateTime.now(),
                List.of(orderItem(1L, "Мяч", 2500L, 2)));

        when(orderService.getOrderById(orderId)).thenReturn(Mono.just(order));

        webTestClient.get()
                .uri("/orders/{id}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("Мяч");
                    assert body.contains("5000");
                });

        verify(orderService, times(1)).getOrderById(orderId);
    }

    @Test
    void getOrder_WithNewOrderFlag_ShouldReturnOrderPage() {
        Long orderId = 1L;
        OrderDto order = order(orderId, 5000L, LocalDateTime.now(),
                List.of(orderItem(1L, "Мяч", 2500L, 2)));

        when(orderService.getOrderById(orderId)).thenReturn(Mono.just(order));

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
                    assert body != null;
                    assert body.contains("Успешная покупка");
                });

        verify(orderService, times(1)).getOrderById(orderId);
    }

    @Test
    void createOrder_WithCartNotEmpty_ShouldCreateOrderAndRedirect() {
        List<ItemDto> cartItems = List.of(cartItem(1L, "Мяч", 2500L, 2));

        OrderDto createdOrder = order(1L, 5000L, LocalDateTime.now(),
                List.of(orderItem(1L, "Мяч", 2500L, 2)));

        when(cartService.getCartItems(anyString())).thenReturn(Flux.fromIterable(cartItems));
        when(cartService.getTotalPrice(anyString())).thenReturn(Mono.just(5000L));
        when(paymentService.pay(5000L)).thenReturn(Mono.just(new PaymentResult(true, "Payment successful")));
        when(orderService.createOrderFromCart(anyString())).thenReturn(Mono.just(createdOrder));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/1?newOrder=true");

        verify(cartService, times(1)).getCartItems(anyString());
        verify(paymentService, times(1)).pay(5000L);
        verify(orderService, times(1)).createOrderFromCart(anyString());
    }

    @Test
    void createOrder_WithEmptyCart_ShouldRedirectToCart() {
        when(cartService.getCartItems(anyString())).thenReturn(Flux.empty());
        when(cartService.getTotalPrice(anyString())).thenReturn(Mono.just(0L));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService, times(1)).getCartItems(anyString());
        verify(paymentService, never()).pay(any());
        verify(orderService, never()).createOrderFromCart(anyString());
    }

    @Test
    void createOrder_WithMultipleItems_ShouldCreateOrderAndRedirect() {
        List<ItemDto> cartItems = List.of(
                cartItem(1L, "Мяч", 2500L, 2),
                cartItem(2L, "Ракетка", 4500L, 1)
        );

        OrderDto createdOrder = order(2L, 9500L, LocalDateTime.now(),
                List.of(orderItem(1L, "Мяч", 2500L, 2),
                        orderItem(2L, "Ракетка", 4500L, 1)));

        when(cartService.getCartItems(anyString())).thenReturn(Flux.fromIterable(cartItems));
        when(cartService.getTotalPrice(anyString())).thenReturn(Mono.just(9500L));
        when(paymentService.pay(9500L)).thenReturn(Mono.just(new PaymentResult(true, "Payment successful")));
        when(orderService.createOrderFromCart(anyString())).thenReturn(Mono.just(createdOrder));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/2?newOrder=true");

        verify(cartService, times(1)).getCartItems(anyString());
        verify(paymentService, times(1)).pay(9500L);
        verify(orderService, times(1)).createOrderFromCart(anyString());
    }

    @Test
    void createOrder_WithPaymentDeclined_ShouldRedirectToCartAndNotCreateOrder() {
        List<ItemDto> cartItems = List.of(cartItem(1L, "Мяч", 2500L, 2));

        when(cartService.getCartItems(anyString())).thenReturn(Flux.fromIterable(cartItems));
        when(cartService.getTotalPrice(anyString())).thenReturn(Mono.just(5000L));
        when(paymentService.pay(5000L)).thenReturn(Mono.just(new PaymentResult(false, "Insufficient funds")));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(paymentService, times(1)).pay(5000L);
        verify(orderService, never()).createOrderFromCart(anyString());
    }

    private static OrderItemDto orderItem(Long id, String title, Long price, int count) {
        return OrderItemDto.builder().id(id).title(title).price(price).count(count).build();
    }

    private static OrderDto order(Long id, Long totalSum, LocalDateTime date, List<OrderItemDto> items) {
        return OrderDto.builder().id(id).orderDate(date).totalSum(totalSum).items(items).build();
    }

    private static ItemDto cartItem(Long id, String title, Long price, int count) {
        return ItemDto.builder().id(id).title(title).price(price).count(count).build();
    }
}