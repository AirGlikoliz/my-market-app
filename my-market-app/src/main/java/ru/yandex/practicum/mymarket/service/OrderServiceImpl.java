package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartSnapshot;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.dto.OrderStatus;
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
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public Mono<OrderDto> createPendingOrder(String username, CartSnapshot snapshot) {
        log.info("Creating pending order for user {}, {} item(s), total {}", username, snapshot.items().size(), snapshot.total());

        Order order = Order.builder()
            .username(username)
            .totalSum(snapshot.total())
            .orderDate(LocalDateTime.now())
            .status(OrderStatus.PENDING)
            .build();

        return orderRepository.save(order)
            .flatMap(savedOrder -> {
                List<OrderItem> orderItems = snapshot.items().stream()
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
                    .map(savedItems -> OrderDto.convertToDto(savedOrder, savedItems));
            });
    }

    @Override
    public Mono<Void> markPaid(Long orderId) {
        return updateStatus(orderId, OrderStatus.PAID);
    }

    @Override
    public Mono<Void> markFailed(Long orderId) {
        return updateStatus(orderId, OrderStatus.FAILED);
    }

    private Mono<Void> updateStatus(Long orderId, OrderStatus status) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with id: " + orderId)))
                .flatMap(order -> {
                    order.setStatus(status);
                    return orderRepository.save(order);
                })
                .then();
    }

    @Override
    public Mono<OrderDto> getOrderById(Long id, String username) {
        log.info("Fetching order by id: {}, user {}", id, username);

        return orderRepository.findByIdAndUsername(id, username)
            .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with id: " + id)))
            .flatMap(order -> orderItemRepository.findAllByOrderId(order.getId())
                .collectList()
                .map(orderItems -> OrderDto.convertToDto(order, orderItems)));
    }

    @Override
    public Flux<OrderDto> getAllOrders(String username) {
        log.info("Fetching all orders, user {}", username);

        return orderRepository.findAllByUsernameOrderByDateDesc(username).map(OrderDto::convertToDto);
    }
}
