package ru.yandex.practicum.mymarket.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DataR2dbcTest
@ActiveProfiles("test")
@Import(MarketUserDetailsService.class)
class MarketUserDetailsServiceTest {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MarketUserDetailsService userDetailsService;

    @Test
    void findByUsername_WithSeededUser_ShouldReturnMatchingUserDetails() {
        StepVerifier.create(userDetailsService.findByUsername("buyer1"))
                .assertNext(userDetails -> {
                    assertEquals("buyer1", userDetails.getUsername());
                    assertTrue(userDetails.isEnabled());
                    assertTrue(PASSWORD_ENCODER.matches("password", userDetails.getPassword()));
                    assertTrue(userDetails.getAuthorities().stream()
                            .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
                })
                .verifyComplete();
    }

    @Test
    void findByUsername_WithWrongPassword_ShouldNotMatch() {
        StepVerifier.create(userDetailsService.findByUsername("buyer1"))
                .assertNext((UserDetails userDetails) ->
                        assertTrue(!PASSWORD_ENCODER.matches("wrong-password", userDetails.getPassword())))
                .verifyComplete();
    }

    @Test
    void findByUsername_WithUnknownUser_ShouldThrowUsernameNotFoundException() {
        StepVerifier.create(userDetailsService.findByUsername("no-such-user"))
                .expectErrorMatches(ex -> ex instanceof UsernameNotFoundException
                        && ex.getMessage().equals("User not found: no-such-user"))
                .verify();
    }
}
