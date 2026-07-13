package com.rentflow.identity.application;

import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.identity.api.LoginRequest;
import com.rentflow.identity.api.LoginResponse;
import com.rentflow.identity.domain.UserAccount;
import com.rentflow.identity.infrastructure.UserMapper;
import com.rentflow.shared.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AuthenticationApplicationService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;
    private final AuditLogWriter auditLogWriter;

    public AuthenticationApplicationService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            JwtProperties properties,
            Clock clock,
            AuditLogWriter auditLogWriter
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
        this.auditLogWriter = auditLogWriter;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest request) {
        UserAccount user = userMapper.findByUsername(request.username()).orElse(null);
        if (user == null || !user.enabled() || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            auditLogWriter.write(new AuditCommand(
                    user == null ? null : user.id(),
                    "LOGIN",
                    "USER",
                    user == null ? null : user.id(),
                    "FAILED",
                    Map.of("username", request.username())
            ));
            throw invalidCredentials();
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(user.id())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("nickname", user.nickname())
                .claim("roles", List.of(user.role()))
                .claim("timezone", user.timezone())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
        )).getTokenValue();
        auditLogWriter.write(new AuditCommand(
                user.id(),
                "LOGIN",
                "USER",
                user.id(),
                "SUCCESS",
                Map.of("role", user.role())
        ));

        return new LoginResponse(
                token,
                "Bearer",
                properties.accessTokenTtl().toSeconds(),
                new LoginResponse.UserSummary(user.id(), user.nickname(), user.role(), user.timezone())
        );
    }

    private static BusinessException invalidCredentials() {
        return new BusinessException(
                "AUTHENTICATION_REQUIRED",
                "Invalid username or password",
                HttpStatus.UNAUTHORIZED
        );
    }
}
