package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.CartSnapshot;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.dto.OrderStatus;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataR2dbcTest
@ActiveProfiles("test")
@Import(OrderServiceImpl.class)
class OrderServiceImplTest {

    private static final String USERNAME = "buyer1";

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
    }

    private static CartSnapshot snapshotOf(ItemDto... items) {
        long total = 0;
        for (ItemDto item : items) {
            total += item.price() * item.count();
        }
        return new CartSnapshot(List.of(items), total);
    }

    private static ItemDto item(Long id, String title, Long price, int count) {
        return ItemDto.builder().id(id).title(title).price(price).count(count).build();
    }

    @Test
    void createPendingOrder_Success() {
        CartSnapshot snapshot = snapshotOf(
                item(1L, "Мяч футбольный", 2500L, 2),
                item(2L, "Теннисная ракетка", 4500L, 1));

        OrderDto result = orderService.createPendingOrder(USERNAME, snapshot).block();

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(9500L, result.totalSum());
        assertEquals(2, result.items().size());
        assertEquals(OrderStatus.PENDING, result.status());

        Order savedOrder = orderRepository.findById(result.id()).block();
        assertNotNull(savedOrder);
        assertEquals(9500L, savedOrder.getTotalSum());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());

        List<OrderItem> savedOrderItems = orderItemRepository.findAllByOrderId(savedOrder.getId())
                .collectList()
                .block();
        assertEquals(2, savedOrderItems.size());
    }

    @Test
    void markPaid_ShouldTransitionOrderToPaid() {
        OrderDto order = orderService.createPendingOrder(USERNAME, snapshotOf(item(1L, "Мяч", 2500L, 1))).block();

        StepVerifier.create(orderService.markPaid(order.id())).verifyComplete();

        Order updated = orderRepository.findById(order.id()).block();
        assertNotNull(updated);
        assertEquals(OrderStatus.PAID, updated.getStatus());
    }

    @Test
    void markFailed_ShouldTransitionOrderToFailed() {
        OrderDto order = orderService.createPendingOrder(USERNAME, snapshotOf(item(1L, "Мяч", 2500L, 1))).block();

        StepVerifier.create(orderService.markFailed(order.id())).verifyComplete();

        Order updated = orderRepository.findById(order.id()).block();
        assertNotNull(updated);
        assertEquals(OrderStatus.FAILED, updated.getStatus());
    }

    @Test
    void markPaid_WhenOrderDoesNotExist_ShouldThrowException() {
        StepVerifier.create(orderService.markPaid(999L))
                .expectErrorMatches(ex -> ex instanceof OrderNotFoundException)
                .verify();
    }

    @Test
    void getOrderById_Success() {
        OrderDto created = orderService.createPendingOrder(USERNAME, snapshotOf(item(1L, "Мяч футбольный", 2500L, 2))).block();

        StepVerifier.create(orderService.getOrderById(created.id(), USERNAME))
                .assertNext(result -> {
                    assertEquals(created.id(), result.id());
                    assertEquals(5000L, result.totalSum());
                    assertEquals(1, result.items().size());
                    assertEquals("Мяч футбольный", result.items().get(0).title());
                    assertEquals(2, result.items().get(0).count());
                })
                .verifyComplete();
    }

    @Test
    void getOrderById_NotFound_ShouldThrowException() {
        Long nonExistingId = 999L;

        StepVerifier.create(orderService.getOrderById(nonExistingId, USERNAME))
                .expectErrorMatches(ex -> ex instanceof OrderNotFoundException
                        && ex.getMessage().equals("Order not found with id: 999"))
                .verify();
    }

    @Test
    void getAllOrders_ShouldReturnAllOrdersSortedByDateDesc() {
        LocalDateTime now = LocalDateTime.now();

        Order order1 = orderRepository.save(Order.builder()
                .username(USERNAME).totalSum(2500L).status(OrderStatus.PAID).orderDate(now.minusMinutes(2)).build()).block();
        Order order2 = orderRepository.save(Order.builder()
                .username(USERNAME).totalSum(5000L).status(OrderStatus.PAID).orderDate(now.minusMinutes(1)).build()).block();
        Order order3 = orderRepository.save(Order.builder()
                .username(USERNAME).totalSum(2500L).status(OrderStatus.PAID).orderDate(now).build()).block();

        List<OrderDto> orders = orderService.getAllOrders(USERNAME).collectList().block();

        assertNotNull(orders);
        assertEquals(3, orders.size());

        assertEquals(order3.getId(), orders.get(0).id());
        assertEquals(order2.getId(), orders.get(1).id());
        assertEquals(order1.getId(), orders.get(2).id());
    }

    @Test
    void getAllOrders_Empty_ShouldReturnEmptyList() {
        StepVerifier.create(orderService.getAllOrders(USERNAME))
                .verifyComplete();
    }

    @Test
    void getOrderById_BelongingToAnotherUser_ShouldThrowException() {
        String otherUsername = "buyer2";
        OrderDto created = orderService.createPendingOrder(USERNAME, snapshotOf(item(1L, "Мяч", 2500L, 1))).block();

        StepVerifier.create(orderService.getOrderById(created.id(), otherUsername))
                .expectErrorMatches(ex -> ex instanceof OrderNotFoundException)
                .verify();
    }

    @Test
    void getAllOrders_ShouldNotIncludeOtherUsersOrders() {
        String otherUsername = "buyer2";

        orderService.createPendingOrder(USERNAME, snapshotOf(item(1L, "Мяч", 2500L, 1))).block();
        orderService.createPendingOrder(otherUsername, snapshotOf(item(1L, "Мяч", 2500L, 1))).block();

        List<OrderDto> ownOrders = orderService.getAllOrders(USERNAME).collectList().block();

        assertNotNull(ownOrders);
        assertEquals(1, ownOrders.size());
    }
}
