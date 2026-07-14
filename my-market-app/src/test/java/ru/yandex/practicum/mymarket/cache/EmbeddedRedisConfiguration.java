package ru.yandex.practicum.mymarket.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
@Slf4j
public class EmbeddedRedisConfiguration {

    private static final RedisServer SHARED_INSTANCE = startSharedInstance();

    @Bean
    public RedisServer redisServer() {
        return SHARED_INSTANCE;
    }

    private static RedisServer startSharedInstance() {
        try {
            RedisServer server = new RedisServer();
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (IOException ignored) {
                }
            }));
            log.info("Embedded Redis started for tests on port {}", server.ports().get(0));
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start embedded Redis for tests", e);
        }
    }
}
