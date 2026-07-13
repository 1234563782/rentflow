package com.rentflow.pricing.api;

import com.rentflow.pricing.application.QuoteApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteController {
    private final QuoteApplicationService quoteApplicationService;

    public QuoteController(QuoteApplicationService quoteApplicationService) {
        this.quoteApplicationService = quoteApplicationService;
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteApplicationService.create(request));
    }
}
