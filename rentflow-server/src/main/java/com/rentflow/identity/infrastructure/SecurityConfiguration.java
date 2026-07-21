package com.rentflow.identity.infrastructure;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.rentflow.identity.application.JwtProperties;
import com.rentflow.identity.application.CorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityErrorWriter errorWriter,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/store/products/*/reviews").authenticated()
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/categories",
                                "/api/v1/catalog/use-cases",
                                "/api/v1/products",
                                "/api/v1/products/**",
                                "/api/v1/store/products/**",
                                "/api/v1/store/skus/**",
                                "/api/v1/availability/search",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/store/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, exception) -> errorWriter.write(
                                response,
                                HttpStatus.UNAUTHORIZED.value(),
                                "AUTHENTICATION_REQUIRED",
                                "Authentication is required"
                        ))
                )
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
                        errorWriter.write(
                                response,
                                HttpStatus.FORBIDDEN.value(),
                                "ACCESS_DENIED",
                                "Access is denied"
                        )
                ));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(properties.allowedOrigins().split(","))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Idempotency-Key",
                "X-Correlation-ID"
        ));
        configuration.setExposedHeaders(List.of("X-Correlation-ID", "Retry-After"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    RsaKeys rsaKeys(JwtProperties properties) {
        if (properties.privateKeyPath() == null || properties.privateKeyPath().isBlank()
                || properties.publicKeyPath() == null || properties.publicKeyPath().isBlank()) {
            throw new IllegalStateException("JWT private and public key paths must be configured");
        }
        try (InputStream privateInput = Files.newInputStream(Path.of(properties.privateKeyPath()));
             InputStream publicInput = Files.newInputStream(Path.of(properties.publicKeyPath()))) {
            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(privateInput);
            RSAPublicKey publicKey = RsaKeyConverters.x509().convert(publicInput);
            if (privateKey == null || publicKey == null) {
                throw new IllegalStateException("JWT RSA keys could not be decoded");
            }
            return new RsaKeys(publicKey, privateKey);
        } catch (IOException exception) {
            throw new IllegalStateException("JWT RSA keys could not be read", exception);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(RsaKeys keys) {
        RSAKey rsaKey = new RSAKey.Builder(keys.publicKey()).privateKey(keys.privateKey()).build();
        JWKSource<SecurityContext> source = (selector, context) -> selector.select(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(source);
    }

    @Bean
    JwtDecoder jwtDecoder(RsaKeys keys, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains(properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token",
                        "Required audience is missing",
                        null
                ));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                audience
        ));
        return decoder;
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    record RsaKeys(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
    }
}
