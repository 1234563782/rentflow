package com.rentflow.identity.api;

public interface CurrentUserProvider {
    CurrentUser requireCurrentUser();
}
