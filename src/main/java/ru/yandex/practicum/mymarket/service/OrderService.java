package ru.yandex.practicum.mymarket.service;

import ru.yandex.practicum.mymarket.dto.OrderDto;

import java.util.List;

public interface OrderService {

    OrderDto createOrderFromCart();

    OrderDto getOrderById(Long id);

    List<OrderDto> getAllOrders();
}