package ru.yandex.practicum.mymarket.service;

import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;

import java.util.Set;

public interface ItemService {

    Mono<Page<ItemDto>> getItems(String search, String sort, int pageNumber, int pageSize);

    Mono<ItemDto> getItemById(Long id);

    Mono<Item> getItemEntityById(Long id);

    Flux<ItemDto> getItemDtosByIds(Set<Long> ids);
}