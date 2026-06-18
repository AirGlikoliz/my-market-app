package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    public Page<ItemDto> getItems(String search, String sort, int pageNumber, int pageSize) {
        log.info("Fetching items - search: {}, sort: {}, page: {}, size: {}", search, sort, pageNumber, pageSize);

        Pageable pageable = createPageable(pageNumber, pageSize, sort);

        Page<Item> itemPage;

        if (search == null || search.isBlank()) {
            itemPage = itemRepository.findAll(pageable);
        } else {
            itemPage = itemRepository.findAllWithSearch(search, pageable);
        }

        return itemPage.map(ItemDto::convertToDto);
    }

    @Override
    public ItemDto getItemById(Long id) {
        log.info("Fetching item by id: {}", id);

        Item item = getItemEntityById(id);
        return ItemDto.convertToDto(item);
    }

    @Override
    public Item getItemEntityById(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
    }

    @Override
    public Map<Long, Item> getItemEntitiesByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();

        return itemRepository.findAllByIdIn(ids).stream().collect(Collectors.toMap(Item::getId, Function.identity()));
    }

    private Pageable createPageable(int pageNumber, int pageSize, String sort) {
        int page = pageNumber - 1;

        Sort sortBy = switch (sort.toUpperCase()) {
            case "ALPHA" -> Sort.by("title").ascending();
            case "PRICE" -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };

        return PageRequest.of(page, pageSize, sortBy);
    }
}
