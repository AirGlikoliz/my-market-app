package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    public Mono<Page<ItemDto>> getItems(String search, String sort, int pageNumber, int pageSize) {

        log.info("Fetching items - search: {}, sort: {}, page: {}, size: {}", search, sort, pageNumber, pageSize);

        if (pageNumber < 1) return Mono.error(new IllegalArgumentException("pageNumber must be >= 1, got: " + pageNumber));

        String normalizedSearch = normalizeSearch(search);
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        String sortKey = mapSort(sort);

        Mono<List<ItemDto>> contentMono = itemRepository
                .findPage(normalizedSearch, sortKey, pageable)
                .map(ItemDto::convertToDto)
                .collectList();

        return Mono.zip(contentMono, itemRepository.countBySearch(normalizedSearch))
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    @Override
    public Mono<ItemDto> getItemById(Long id) {
        log.info("Fetching item by id: {}", id);

        return getItemEntityById(id).map(ItemDto::convertToDto);
    }

    @Override
    public Mono<Item> getItemEntityById(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found with id: " + id)));
    }

    @Override
    public Flux<Item> getItemEntitiesByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Flux.empty();
        return itemRepository.findAllByIdIn(ids);
    }

    private String normalizeSearch(String search) {
        return (search == null || search.isBlank()) ? "" : search;
    }

    private String mapSort(String sort) {
        if (sort == null) return "NONE";

        return switch (sort.toUpperCase()) {
            case "ALPHA" -> "TITLE_ASC";
            case "PRICE" -> "PRICE_ASC";
            default -> "NONE";
        };
    }
}