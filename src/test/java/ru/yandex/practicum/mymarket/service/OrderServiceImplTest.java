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
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({OrderServiceImpl.class, CartServiceImpl.class, ItemServiceImpl.class})
class OrderServiceImplTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemRepository itemRepository;

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
        cartService.clearCart().block();

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
        // given
        Long item1Id = savedItems.get(0).getId();
        Long item2Id = savedItems.get(1).getId();

        cartService.increaseQuantity(item1Id);
        cartService.increaseQuantity(item1Id);
        cartService.increaseQuantity(item2Id);

        // when / then
        OrderDto result = orderService.createOrderFromCart().block();

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(9500L, result.totalSum());
        assertEquals(2, result.items().size());

        List<Item> cartLeft = cartService.getCartItems().collectList().block()
                .stream().map(dto -> (Item) null).toList();
        assertTrue(cartService.getCartItems().collectList().block().isEmpty());

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
        // when / then
        StepVerifier.create(orderService.createOrderFromCart())
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("Cannot create empty order"))
                .verify();

        assertEquals(0L, orderRepository.count().block());
    }

    @Test
    void createOrderFromCart_WithSingleItem_Success() {
        // given
        Long itemId = savedItems.get(0).getId();

        cartService.increaseQuantity(itemId);

        // when
        OrderDto result = orderService.createOrderFromCart().block();

        // then
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(2500L, result.totalSum());
        assertEquals(1, result.items().size());
        assertEquals("Мяч футбольный", result.items().get(0).title());
        assertEquals(1, result.items().get(0).count());
        assertEquals(2500L, result.items().get(0).price());

        assertTrue(cartService.getCartItems().collectList().block().isEmpty());
    }

    @Test
    void getOrderById_Success() {
        // given
        Long itemId = savedItems.get(0).getId();

        cartService.increaseQuantity(itemId);
        cartService.increaseQuantity(itemId);

        OrderDto createdOrder = orderService.createOrderFromCart().block();

        // when / then
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
        // given
        Long nonExistingId = 999L;

        // when / then
        StepVerifier.create(orderService.getOrderById(nonExistingId))
                .expectErrorMatches(ex -> ex instanceof OrderNotFoundException
                        && ex.getMessage().equals("Order not found with id: 999"))
                .verify();
    }

    @Test
    void getAllOrders_ShouldReturnAllOrdersSortedByDateDesc() throws InterruptedException {
        // given
        Long itemId = savedItems.get(0).getId();

        cartService.increaseQuantity(itemId);
        OrderDto order1 = orderService.createOrderFromCart().block();

        Thread.sleep(100);

        cartService.increaseQuantity(itemId);
        cartService.increaseQuantity(itemId);
        OrderDto order2 = orderService.createOrderFromCart().block();

        Thread.sleep(100);

        cartService.increaseQuantity(itemId);
        OrderDto order3 = orderService.createOrderFromCart().block();

        // when
        List<OrderDto> orders = orderService.getAllOrders().collectList().block();

        // then
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
        // when / then
        StepVerifier.create(orderService.getAllOrders())
                .verifyComplete();
    }

    @Test
    void createOrder_ShouldClearCartAfterCreation() {
        // given
        Long itemId = savedItems.get(0).getId();

        cartService.increaseQuantity(itemId);
        cartService.increaseQuantity(itemId);
        assertFalse(cartService.getCartItems().collectList().block().isEmpty());

        // when
        orderService.createOrderFromCart().block();

        // then
        assertTrue(cartService.getCartItems().collectList().block().isEmpty());
    }
}