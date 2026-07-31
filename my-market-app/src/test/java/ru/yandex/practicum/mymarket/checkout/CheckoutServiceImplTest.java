package ru.yandex.practicum.mymarket.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.*;
import ru.yandex.practicum.mymarket.dto.OrderStatus;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentService;
import ru.yandex.practicum.mymarket.service.checkout.CheckoutServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    private static final String USERNAME = "buyer1";

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    private CheckoutServiceImpl checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutServiceImpl(cartService, orderService, paymentService);
    }

    private static ItemDto item(Long id, String title, Long price, int count) {
        return ItemDto.builder().id(id).title(title).price(price).count(count).build();
    }

    private static OrderDto pendingOrder(Long id, Long total) {
        return OrderDto.builder().id(id).totalSum(total).items(List.of()).status(OrderStatus.PENDING).build();
    }

    @Test
    void checkout_WithEmptyCart_ShouldReturnEmptyCartResult() {
        when(cartService.getCartItems(USERNAME)).thenReturn(Flux.empty());

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(result -> assertEquals(CheckoutResult.emptyCart(), result))
                .verifyComplete();

        verifyNoInteractions(orderService, paymentService);
    }

    @Test
    void checkout_WithSuccessfulPayment_ShouldMarkOrderPaidAndClearCart() {
        List<ItemDto> cartItems = List.of(item(1L, "Мяч", 2500L, 2));
        when(cartService.getCartItems(USERNAME)).thenReturn(Flux.fromIterable(cartItems));
        when(orderService.createPendingOrder(eq(USERNAME), eq(new CartSnapshot(cartItems, 5000L))))
                .thenReturn(Mono.just(pendingOrder(1L, 5000L)));
        when(paymentService.pay(USERNAME, 5000L)).thenReturn(Mono.just(new PaymentResult(true, "Payment successful")));
        when(orderService.markPaid(1L)).thenReturn(Mono.empty());
        when(cartService.clearCart(USERNAME)).thenReturn(Mono.empty());

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(result -> assertEquals(CheckoutResult.success(1L), result))
                .verifyComplete();

        verify(orderService).markPaid(1L);
        verify(orderService, never()).markFailed(any());
        verify(cartService).clearCart(USERNAME);
    }

    @Test
    void checkout_WithDeclinedPayment_ShouldMarkOrderFailedAndKeepCart() {
        List<ItemDto> cartItems = List.of(item(1L, "Мяч", 2500L, 2));
        when(cartService.getCartItems(USERNAME)).thenReturn(Flux.fromIterable(cartItems));
        when(orderService.createPendingOrder(eq(USERNAME), any())).thenReturn(Mono.just(pendingOrder(1L, 5000L)));
        when(paymentService.pay(USERNAME, 5000L)).thenReturn(Mono.just(new PaymentResult(false, "Insufficient funds")));
        when(orderService.markFailed(1L)).thenReturn(Mono.empty());

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(result -> assertEquals(CheckoutResult.paymentDeclined(), result))
                .verifyComplete();

        verify(orderService).markFailed(1L);
        verify(orderService, never()).markPaid(any());
        verify(cartService, never()).clearCart(any());
    }

    @Test
    void checkout_WhenMarkPaidFailsAfterSuccessfulPayment_ShouldStillReturnSuccessAndAttemptCartClear() {
        List<ItemDto> cartItems = List.of(item(1L, "Мяч", 2500L, 2));
        when(cartService.getCartItems(USERNAME)).thenReturn(Flux.fromIterable(cartItems));
        when(orderService.createPendingOrder(eq(USERNAME), any())).thenReturn(Mono.just(pendingOrder(1L, 5000L)));
        when(paymentService.pay(USERNAME, 5000L)).thenReturn(Mono.just(new PaymentResult(true, "Payment successful")));
        when(orderService.markPaid(1L)).thenReturn(Mono.error(new RuntimeException("db unavailable")));
        when(cartService.clearCart(USERNAME)).thenReturn(Mono.empty());

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(result -> assertEquals(CheckoutResult.success(1L), result))
                .verifyComplete();

        verify(cartService).clearCart(USERNAME);
    }

    @Test
    void checkout_WhenClearCartFailsAfterSuccessfulPayment_ShouldStillReturnSuccess() {
        List<ItemDto> cartItems = List.of(item(1L, "Мяч", 2500L, 2));
        when(cartService.getCartItems(USERNAME)).thenReturn(Flux.fromIterable(cartItems));
        when(orderService.createPendingOrder(eq(USERNAME), any())).thenReturn(Mono.just(pendingOrder(1L, 5000L)));
        when(paymentService.pay(USERNAME, 5000L)).thenReturn(Mono.just(new PaymentResult(true, "Payment successful")));
        when(orderService.markPaid(1L)).thenReturn(Mono.empty());
        when(cartService.clearCart(USERNAME)).thenReturn(Mono.error(new RuntimeException("db unavailable")));

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(result -> assertEquals(CheckoutResult.success(1L), result))
                .verifyComplete();
    }

    @Test
    void checkout_WhenMarkFailedFailsAfterDeclinedPayment_ShouldStillReturnPaymentDeclined() {
        List<ItemDto> cartItems = List.of(item(1L, "Мяч", 2500L, 2));
        when(cartService.getCartItems(USERNAME)).thenReturn(Flux.fromIterable(cartItems));
        when(orderService.createPendingOrder(eq(USERNAME), any())).thenReturn(Mono.just(pendingOrder(1L, 5000L)));
        when(paymentService.pay(USERNAME, 5000L)).thenReturn(Mono.just(new PaymentResult(false, "Insufficient funds")));
        when(orderService.markFailed(1L)).thenReturn(Mono.error(new RuntimeException("db unavailable")));

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(result -> assertEquals(CheckoutResult.paymentDeclined(), result))
                .verifyComplete();
    }

    @Test
    void checkout_ShouldComputeTotalFromSnapshotWithoutRereadingCart() {
        List<ItemDto> cartItems = List.of(item(1L, "Мяч", 2500L, 2), item(2L, "Ракетка", 4500L, 1));
        when(cartService.getCartItems(USERNAME)).thenReturn(Flux.fromIterable(cartItems));
        when(orderService.createPendingOrder(eq(USERNAME), eq(new CartSnapshot(cartItems, 9500L))))
                .thenReturn(Mono.just(pendingOrder(1L, 9500L)));
        when(paymentService.pay(USERNAME, 9500L)).thenReturn(Mono.just(new PaymentResult(true, "Payment successful")));
        when(orderService.markPaid(1L)).thenReturn(Mono.empty());
        when(cartService.clearCart(USERNAME)).thenReturn(Mono.empty());

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .expectNextCount(1)
                .verifyComplete();

        verify(cartService, times(1)).getCartItems(USERNAME);
        verify(cartService, never()).getTotalPrice(any());
    }
}
