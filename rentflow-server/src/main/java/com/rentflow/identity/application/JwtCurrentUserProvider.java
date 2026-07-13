package com.rentflow.identity.application;

import com.rentflow.identity.api.CurrentUser;
import com.rentflow.identity.api.CurrentUserProvider;
import com.rentflow.shared.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {
    @Override
    public CurrentUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !authentication.isAuthenticated()) {
            throw new BusinessException(
                    "AUTHENTICATION_REQUIRED",
                    "Authentication is required",
                    HttpStatus.UNAUTHORIZED
            );
        }

        List<String> roles = token.getToken().getClaimAsStringList("roles");
        return new CurrentUser(
                token.getToken().getSubject(),
                token.getToken().getClaimAsString("nickname"),
                roles == null || roles.isEmpty() ? "USER" : roles.getFirst(),
                token.getToken().getClaimAsString("timezone")
        );
    }
}
