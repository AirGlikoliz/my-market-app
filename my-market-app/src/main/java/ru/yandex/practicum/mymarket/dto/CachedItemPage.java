package ru.yandex.practicum.mymarket.dto;

import java.util.List;

public record CachedItemPage(List<ItemDto> content, long totalElements) {
}
