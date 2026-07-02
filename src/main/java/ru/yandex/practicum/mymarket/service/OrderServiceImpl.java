package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public Mono<OrderDto> createOrderFromCart() {
        log.info("Creating order from cart");

        return cartService.getCartItems()
            .collectList()
            .flatMap(cartItems -> {
                if (cartItems.isEmpty()) return Mono.error(new IllegalArgumentException("Cannot create empty order"));
                return cartService.getTotalPrice()
                    .flatMap(totalSum -> {
                        Order order = Order.builder().totalSum(totalSum).orderDate(LocalDateTime.now()).build();
                        return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                List<OrderItem> orderItems = cartItems.stream()
                                    .map(item -> OrderItem.builder()
                                        .itemId(item.id())
                                        .title(item.title())
                                        .price(item.price())
                                        .count(item.count())
                                        .orderId(savedOrder.getId())
                                        .build())
                                    .collect(Collectors.toList());
                                return orderItemRepository.saveAll(orderItems)
                                    .collectList()
                                    .then(cartService.clearCart())
                                    .thenReturn(OrderDto.convertToDto(savedOrder, orderItems));
                            });
                    });
            });
    }

    @Override
    public Mono<OrderDto> getOrderById(Long id) {
        log.info("Fetching order by id: {}", id);

        return orderRepository.findById(id)
            .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with id: " + id)))
            .flatMap(order -> orderItemRepository.findAllByOrderId(order.getId())
                .collectList()
                .map(orderItems -> OrderDto.convertToDto(order, orderItems)));
    }

    @Override
    public Flux<OrderDto> getAllOrders() {
        log.info("Fetching all orders");

        return orderRepository.findAllOrderByDateDesc().map(OrderDto::convertToDto);
    }
}