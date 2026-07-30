package ru.yandex.practicum.mymarket.dto;

import java.util.List;

public record CartSnapshot(List<ItemDto> items, Long total) {

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
