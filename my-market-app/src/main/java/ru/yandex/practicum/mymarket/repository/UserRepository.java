package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.AppUser;

public interface UserRepository extends ReactiveCrudRepository<AppUser, String> {

    Mono<AppUser> findByUsername(String username);
}
