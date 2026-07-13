package com.rentflow.identity.infrastructure;

import com.rentflow.identity.application.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRoundTripTest {
    @Test
    void signsAndVerifiesRequiredClaimsWithTemporaryRsaKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        SecurityConfiguration configuration = new SecurityConfiguration();
        SecurityConfiguration.RsaKeys keys = new SecurityConfiguration.RsaKeys(
                (RSAPublicKey) pair.getPublic(),
                (RSAPrivateKey) pair.getPrivate()
        );
        JwtProperties properties = new JwtProperties(
                "unused-private.pem",
                "unused-public.pem",
                "rentflow-server",
                "rentflow-platform",
                Duration.ofMinutes(30)
        );
        JwtEncoder encoder = configuration.jwtEncoder(keys);
        JwtDecoder decoder = configuration.jwtDecoder(keys, properties);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject("01J00000000000000000000001")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("nickname", "Demo")
                .claim("roles", List.of("USER"))
                .claim("timezone", "Asia/Shanghai")
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
        )).getTokenValue();
        Jwt decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("01J00000000000000000000001");
        assertThat(decoded.getAudience()).containsExactly("rentflow-platform");
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(decoded.getClaimAsString("timezone")).isEqualTo("Asia/Shanghai");
    }
}
