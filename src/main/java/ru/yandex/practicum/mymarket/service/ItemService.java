package ru.yandex.practicum.mymarket.service;

import org.springframework.data.domain.Page;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;

public interface ItemService {

    Page<ItemDto> getItems(String search, String sort, int pageNumber, int pageSize);

    ItemDto getItemById(Long id);

    Item getItemEntityById(Long id);
}
