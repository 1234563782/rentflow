package com.rentflow.identity.infrastructure;

import com.rentflow.identity.application.DemoUserProperties;
import com.rentflow.identity.domain.UserAccount;
import com.rentflow.shared.id.Ulid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DemoUserSeeder implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoUserSeeder.class);
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DemoUserProperties properties;

    public DemoUserSeeder(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            DemoUserProperties properties
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.password() == null || properties.password().isBlank()) {
            LOGGER.info("Demo user seeding skipped because RENTFLOW_DEMO_PASSWORD is empty");
            return;
        }
        if (properties.username() == null || properties.username().isBlank()) {
            throw new IllegalStateException("rentflow.demo-user.username must be configured");
        }
        if (userMapper.findByUsername(properties.username()).isPresent()) {
            return;
        }
        UserAccount account = new UserAccount(
                Ulid.next(),
                properties.username(),
                passwordEncoder.encode(properties.password()),
                properties.nickname(),
                "USER",
                properties.timezone(),
                true
        );
        if (userMapper.insert(account) != 1) {
            throw new IllegalStateException("Demo user insert did not affect exactly one row");
        }
        LOGGER.info("Created development demo user username={}", properties.username());
    }
}
