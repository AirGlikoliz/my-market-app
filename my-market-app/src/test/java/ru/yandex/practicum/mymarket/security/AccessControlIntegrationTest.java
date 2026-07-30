package ru.yandex.practicum.mymarket.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cache.EmbeddedRedisConfiguration;
import ru.yandex.practicum.mymarket.dto.PaymentResult;
import ru.yandex.practicum.mymarket.dto.PaymentStatus;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.PaymentService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(EmbeddedRedisConfiguration.class)
class AccessControlIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ItemRepository itemRepository;

    @MockBean
    private PaymentService paymentService;

    private Long itemId;

    @BeforeEach
    void setUp() {
        Item item = itemRepository.save(Item.builder()
                .title("Мяч футбольный")
                .description("Профессиональный футбольный мяч")
                .imgPath("/images/ball.jpg")
                .price(2500L)
                .build()).block();
        itemId = item.getId();

        when(paymentService.checkBalance(anyString(), any()))
                .thenReturn(Mono.just(PaymentStatus.builder().available(true).sufficientFunds(true).balance(100000L).build()));
        when(paymentService.pay(anyString(), any()))
                .thenReturn(Mono.just(new PaymentResult(true, "Payment successful")));
    }

    @Test
    void anonymous_canBrowseItemList() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void anonymous_canViewSingleItem() {
        webTestClient.get().uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void anonymous_cannotOpenCart_redirectsToLogin() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", location -> assertTrue(location.contains("/login")));
    }

    @Test
    void anonymous_cannotOpenOrders_redirectsToLogin() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", location -> assertTrue(location.contains("/login")));
    }

    @Test
    void anonymous_cannotAddItemToCart_isForbidden() {
        webTestClient.post().uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", String.valueOf(itemId)).with("action", "PLUS"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void anonymous_cannotCheckout_isForbidden() {
        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void authenticatedUser_canOpenCart() {
        webTestClient.mutateWith(mockUser("buyer1")).get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void authenticatedUser_canOpenOrders() {
        webTestClient.mutateWith(mockUser("buyer1")).get().uri("/orders")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void authenticatedUser_canAddItemToCart() {
        webTestClient.mutateWith(mockUser("buyer1")).mutateWith(csrf())
                .post().uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", String.valueOf(itemId)).with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", location -> assertFalse(location.contains("/login")));
    }

    @Test
    void authenticatedUser_withoutCsrfToken_isForbidden() {
        webTestClient.mutateWith(mockUser("buyer1"))
                .post().uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", String.valueOf(itemId)).with("action", "PLUS"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
