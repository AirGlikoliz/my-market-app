package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;

    @Override
    @Transactional
    public OrderDto createOrderFromCart() {
        log.info("Creating order from cart");

        List<ItemDto> cartItems = cartService.getCartItems();

        if (cartItems.isEmpty()) throw new IllegalArgumentException("Cannot create empty order");

        Long totalSum = cartService.getTotalPrice();

        Order order = Order.builder().totalSum(totalSum).build();

        List<OrderItem> orderItems = cartItems.stream()
                .map(item -> OrderItem.builder()
                        .itemId(item.id())
                        .title(item.title())
                        .price(item.price())
                        .count(item.count())
                        .order(order)
                        .build())
                .collect(Collectors.toList());

        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        cartService.clearCart();

        log.info("Order created successfully with id: {}", savedOrder.getId());
        return OrderDto.convertToDto(savedOrder);
    }

    @Override
    public OrderDto getOrderById(Long id) {
        log.info("Fetching order by id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        return OrderDto.convertToDto(order);
    }

    @Override
    public List<OrderDto> getAllOrders() {
        log.info("Fetching all orders");

        List<Order> orders = orderRepository.findAllOrderByDateDesc();

        return orders.stream()
                .map(OrderDto::convertToDto)
                .collect(Collectors.toList());
    }
}
