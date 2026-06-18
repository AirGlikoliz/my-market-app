package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CartService cartService;

    @Test
    void getOrders_ShouldReturnOrdersPage() throws Exception {
        // given
        List<OrderDto> orders = List.of(
            OrderDto.builder()
                .id(1L)
                .orderDate(LocalDateTime.now().minusDays(1))
                .totalSum(5000L)
                .items(List.of(
                    OrderItemDto.builder()
                        .id(1L)
                        .title("Мяч")
                        .price(2500L)
                        .count(2)
                        .build()
                )).build(),
            OrderDto.builder()
                .id(2L)
                .orderDate(LocalDateTime.now())
                .totalSum(9500L)
                .items(List.of(
                    OrderItemDto.builder()
                        .id(1L)
                        .title("Мяч")
                        .price(2500L)
                        .count(2)
                        .build(),
                    OrderItemDto.builder()
                        .id(2L)
                        .title("Ракетка")
                        .price(4500L)
                        .count(1)
                        .build()
                )).build()
        );

        when(orderService.getAllOrders()).thenReturn(orders);

        // when & then
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", orders));

        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void getOrders_WhenNoOrders_ShouldReturnEmptyPage() throws Exception {
        // given
        when(orderService.getAllOrders()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", List.of()));

        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void getOrder_ShouldReturnOrderPage() throws Exception {
        // given
        Long orderId = 1L;
        OrderDto order = OrderDto.builder()
            .id(orderId)
            .orderDate(LocalDateTime.now())
            .totalSum(5000L)
            .items(List.of(
                OrderItemDto.builder()
                    .id(1L)
                    .title("Мяч")
                    .price(2500L)
                    .count(2)
                    .build()
            )).build();

        when(orderService.getOrderById(orderId)).thenReturn(order);

        // when & then
        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attributeExists("newOrder"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("newOrder", false));

        verify(orderService, times(1)).getOrderById(orderId);
    }

    @Test
    void getOrder_WithNewOrderFlag_ShouldReturnOrderPage() throws Exception {
        // given
        Long orderId = 1L;
        OrderDto order = OrderDto.builder()
            .id(orderId)
            .orderDate(LocalDateTime.now())
            .totalSum(5000L)
            .items(List.of(
                OrderItemDto.builder()
                    .id(1L)
                    .title("Мяч")
                    .price(2500L)
                    .count(2)
                    .build()
            )).build();

        when(orderService.getOrderById(orderId)).thenReturn(order);

        // when & then
        mockMvc.perform(get("/orders/{id}", orderId)
                        .param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("newOrder", true));

        verify(orderService, times(1)).getOrderById(orderId);
    }

    @Test
    void createOrder_WithCartNotEmpty_ShouldCreateOrderAndRedirect() throws Exception {
        // given
        List<ItemDto> cartItems = List.of(
            ItemDto.builder()
                .id(1L)
                .title("Мяч")
                .price(2500L)
                .count(2)
                .build()
        );

        OrderDto createdOrder = OrderDto.builder()
            .id(1L)
            .orderDate(LocalDateTime.now())
            .totalSum(5000L)
            .items(List.of(
                OrderItemDto.builder()
                    .id(1L)
                    .title("Мяч")
                    .price(2500L)
                    .count(2)
                    .build()
            ))
            .build();

        when(cartService.getCartItems()).thenReturn(cartItems);
        when(orderService.createOrderFromCart()).thenReturn(createdOrder);

        // when & then
        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1?newOrder=true"));

        verify(cartService, times(1)).getCartItems();
        verify(orderService, times(1)).createOrderFromCart();
    }

    @Test
    void createOrder_WithEmptyCart_ShouldRedirectToCart() throws Exception {
        // given
        when(cartService.getCartItems()).thenReturn(List.of());

        // when & then
        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService, times(1)).getCartItems();
        verify(orderService, never()).createOrderFromCart();
    }

    @Test
    void createOrder_WithMultipleItems_ShouldCreateOrderAndRedirect() throws Exception {
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

        OrderDto createdOrder = OrderDto.builder()
            .id(2L)
            .orderDate(LocalDateTime.now())
            .totalSum(9500L)
            .items(List.of(
                OrderItemDto.builder()
                    .id(1L)
                    .title("Мяч")
                    .price(2500L)
                    .count(2)
                    .build(),
                OrderItemDto.builder()
                    .id(2L)
                    .title("Ракетка")
                    .price(4500L)
                    .count(1)
                    .build()
            ))
            .build();

        when(cartService.getCartItems()).thenReturn(cartItems);
        when(orderService.createOrderFromCart()).thenReturn(createdOrder);

        // when & then
        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/2?newOrder=true"));

        verify(cartService, times(1)).getCartItems();
        verify(orderService, times(1)).createOrderFromCart();
    }
}
