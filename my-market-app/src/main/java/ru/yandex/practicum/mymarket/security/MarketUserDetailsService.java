package ru.yandex.practicum.mymarket.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class MarketUserDetailsService implements ReactiveUserDetailsService {

    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .map(user -> User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                        .roles(ROLE_CUSTOMER)
                        .build());
    }
}
