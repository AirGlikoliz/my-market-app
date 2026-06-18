package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//Использовал эту аннотацию потому что в CartServiceImpl стоит @SessionScope
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({OrderServiceImpl.class, CartServiceImpl.class, ItemServiceImpl.class})
class OrderServiceImplTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();

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

        itemRepository.saveAll(List.of(item1, item2));
    }

    @Test
    void createOrderFromCart_Success() {
        // given
        List<Item> items = itemRepository.findAll();
        Long item1Id = items.get(0).getId();
        Long item2Id = items.get(1).getId();

        cartService.increaseQuantity(item1Id);
        cartService.increaseQuantity(item1Id);
        cartService.increaseQuantity(item2Id);

        // when
        OrderDto result = orderService.createOrderFromCart();

        // then
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(9500L, result.totalSum());
        assertEquals(2, result.items().size());

        assertTrue(cartService.getCartItems().isEmpty());

        Order savedOrder = orderRepository.findById(result.id()).orElseThrow();
        assertEquals(9500L, savedOrder.getTotalSum());
        assertEquals(2, savedOrder.getItems().size());
    }

    @Test
    void createOrderFromCart_EmptyCart_ShouldThrowException() {
        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrderFromCart());

        assertEquals("Cannot create empty order", exception.getMessage());

        assertEquals(0, orderRepository.count());
    }

    @Test
    void createOrderFromCart_WithSingleItem_Success() {
        // given
        List<Item> items = itemRepository.findAll();
        Long itemId = items.get(0).getId();

        cartService.increaseQuantity(itemId);

        // when
        OrderDto result = orderService.createOrderFromCart();

        // then
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(2500L, result.totalSum());
        assertEquals(1, result.items().size());
        assertEquals("Мяч футбольный", result.items().get(0).title());
        assertEquals(1, result.items().get(0).count());
        assertEquals(2500L, result.items().get(0).price());

        assertTrue(cartService.getCartItems().isEmpty());
    }

    @Test
    void getOrderById_Success() {
        // given
        List<Item> items = itemRepository.findAll();
        Long itemId = items.get(0).getId();

        cartService.increaseQuantity(itemId);
        cartService.increaseQuantity(itemId);

        OrderDto createdOrder = orderService.createOrderFromCart();

        // when
        OrderDto result = orderService.getOrderById(createdOrder.id());

        // then
        assertNotNull(result);
        assertEquals(createdOrder.id(), result.id());
        assertEquals(5000L, result.totalSum());
        assertEquals(1, result.items().size());
        assertEquals("Мяч футбольный", result.items().get(0).title());
        assertEquals(2, result.items().get(0).count());
    }

    @Test
    void getOrderById_NotFound_ShouldThrowException() {
        // given
        Long nonExistingId = 999L;

        // when & then
        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderById(nonExistingId));

        assertEquals("Order not found with id: 999", exception.getMessage());
    }

    @Test
    void getAllOrders_ShouldReturnAllOrdersSortedByDateDesc() throws InterruptedException {
        // given
        List<Item> items = itemRepository.findAll();
        Long itemId = items.get(0).getId();

        cartService.increaseQuantity(itemId);
        OrderDto order1 = orderService.createOrderFromCart();

        Thread.sleep(100);

        cartService.increaseQuantity(itemId);
        cartService.increaseQuantity(itemId);
        OrderDto order2 = orderService.createOrderFromCart();

        Thread.sleep(100);

        cartService.increaseQuantity(itemId);
        OrderDto order3 = orderService.createOrderFromCart();

        // when
        List<OrderDto> orders = orderService.getAllOrders();

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
        // when
        List<OrderDto> orders = orderService.getAllOrders();

        // then
        assertNotNull(orders);
        assertTrue(orders.isEmpty());
    }

    @Test
    void createOrder_ShouldClearCartAfterCreation() {
        // given
        List<Item> items = itemRepository.findAll();
        Long itemId = items.get(0).getId();

        cartService.increaseQuantity(itemId);
        cartService.increaseQuantity(itemId);
        assertFalse(cartService.getCartItems().isEmpty());

        // when
        orderService.createOrderFromCart();

        // then
        assertTrue(cartService.getCartItems().isEmpty());
    }
}
