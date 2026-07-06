package ru.yandex.practicum.mymarket.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartCleanupScheduler {

    private final CartItemRepository cartItemRepository;

    private static final int CART_TTL_MINUTES = 30;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cleanupExpiredCartItems() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(CART_TTL_MINUTES);
        log.info("Cleaning up cart items older than {}", deadline);

        cartItemRepository.deleteAllOlderThan(deadline)
                .doOnSuccess(v -> log.info("Cart cleanup completed"))
                .doOnError(e -> log.error("Cart cleanup failed", e))
                .subscribe();
    }
}