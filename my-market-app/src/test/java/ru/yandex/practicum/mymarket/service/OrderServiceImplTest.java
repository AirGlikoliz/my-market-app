package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({OrderServiceImpl.class, CartServiceImpl.class, ItemServiceImpl.class})
class OrderServiceImplTest {

    private static final String SESSION_ID = "test-session";

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    private List<Item> savedItems;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        itemRepository.deleteAll().block();
        cartItemRepository.deleteAll().block();

        Item item1 = Item.builder()
                .title("Мяч футбольный")
                .description("Профессиональный футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .build();

        Item item2 = Item.builder()
                .title("Теннисная ракетка")
                .description("Облегченная теннисная ракетка")
                .imgPath("/images/racket.jpg")
                .price(4500L)
                .build();

        savedItems = itemRepository.saveAll(List.of(item1, item2))
                .collectList()
                .block();
    }

    @Test
    void createOrderFromCart_Success() {
        Long item1Id = savedItems.get(0).getId();
        Long item2Id = savedItems.get(1).getId();

        cartService.increaseQuantity(SESSION_ID, item1Id).block();
        cartService.increaseQuantity(SESSION_ID, item1Id).block();
        cartService.increaseQuantity(SESSION_ID, item2Id).block();

        OrderDto result = orderService.createOrderFromCart(SESSION_ID).block();

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(9500L, result.totalSum());
        assertEquals(2, result.items().size());

        assertTrue(cartService.getCartItems(SESSION_ID).collectList().block().isEmpty());

        Order savedOrder = orderRepository.findById(result.id()).block();
        assertNotNull(savedOrder);
        assertEquals(9500L, savedOrder.getTotalSum());

        List<OrderItem> savedOrderItems = orderItemRepository.findAllByOrderId(savedOrder.getId())
                .collectList()
                .block();
        assertEquals(2, savedOrderItems.size());
    }

    @Test
    void createOrderFromCart_EmptyCart_ShouldThrowException() {
        StepVerifier.create(orderService.createOrderFromCart(SESSION_ID))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("Cannot create empty order"))
                .verify();

        assertEquals(0L, orderRepository.count().block());
    }

    @Test
    void createOrderFromCart_WithSingleItem_Success() {
        Long itemId = savedItems.get(0).getId();

        cartService.increaseQuantity(SESSION_ID, itemId).block();

        OrderDto result = orderService.createOrderFromCart(SESSION_ID).block();

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(2500L, result.totalSum());
        assertEquals(1, result.items().size());
        assertEquals("Мяч футбольный", result.items().get(0).title());
        assertEquals(1, result.items().get(0).count());
        assertEquals(2500L, result.items().get(0).price());

        assertTrue(cartService.getCartItems(SESSION_ID).collectList().block().isEmpty());
    }

    @Test
    void getOrderById_Success() {
        Long itemId = savedItems.get(0).getId();

        cartService.increaseQuantity(SESSION_ID, itemId).block();
        cartService.increaseQuantity(SESSION_ID, itemId).block();

        OrderDto createdOrder = orderService.createOrderFromCart(SESSION_ID).block();

        StepVerifier.create(orderService.getOrderById(createdOrder.id()))
                .assertNext(result -> {
                    assertEquals(createdOrder.id(), result.id());
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

        StepVerifier.create(orderService.getOrderById(nonExistingId))
                .expectErrorMatches(ex -> ex instanceof OrderNotFoundException
                        && ex.getMessage().equals("Order not found with id: 999"))
                .verify();
    }

    @Test
    void getAllOrders_ShouldReturnAllOrdersSortedByDateDesc() throws InterruptedException {
        Long itemId = savedItems.get(0).getId();

        cartService.increaseQuantity(SESSION_ID, itemId).block();
        OrderDto order1 = orderService.createOrderFromCart(SESSION_ID).block();

        Thread.sleep(100);

        cartService.increaseQuantity(SESSION_ID, itemId).block();
        cartService.increaseQuantity(SESSION_ID, itemId).block();
        OrderDto order2 = orderService.createOrderFromCart(SESSION_ID).block();

        Thread.sleep(100);

        cartService.increaseQuantity(SESSION_ID, itemId).block();
        OrderDto order3 = orderService.createOrderFromCart(SESSION_ID).block();

        List<OrderDto> orders = orderService.getAllOrders().collectList().block();

        assertNotNull(orders);
        assertEquals(3, orders.size());

        assertEquals(order3.id(), orders.get(0).id());
        assertEquals(order2.id(), orders.get(1).id());
        assertEquals(order1.id(), orders.get(2).id());

        assertEquals(2500L, orders.get(0).totalSum());
        assertEquals(5000L, orders.get(1).totalSum());
        assertEquals(2500L, orders.get(2).totalSum());
    }

    @Test
    void getAllOrders_Empty_ShouldReturnEmptyList() {
        StepVerifier.create(orderService.getAllOrders())
                .verifyComplete();
    }

    @Test
    void createOrder_ShouldClearCartAfterCreation() {
        Long itemId = savedItems.get(0).getId();

        cartService.increaseQuantity(SESSION_ID, itemId).block();
        cartService.increaseQuantity(SESSION_ID, itemId).block();
        assertFalse(cartService.getCartItems(SESSION_ID).collectList().block().isEmpty());

        orderService.createOrderFromCart(SESSION_ID).block();

        assertTrue(cartService.getCartItems(SESSION_ID).collectList().block().isEmpty());
    }
}