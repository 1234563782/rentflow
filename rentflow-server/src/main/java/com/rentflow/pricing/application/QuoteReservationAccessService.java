package com.rentflow.pricing.application;

import com.rentflow.pricing.api.LockedQuote;
import com.rentflow.pricing.api.QuoteReservationAccess;
import com.rentflow.pricing.infrastructure.QuoteMapper;
import com.rentflow.shared.id.Ulid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class QuoteReservationAccessService implements QuoteReservationAccess {
    private final QuoteMapper quoteMapper;

    public QuoteReservationAccessService(QuoteMapper quoteMapper) {
        this.quoteMapper = quoteMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<LockedQuote> lockQuote(String quoteId) {
        return quoteMapper.lockById(Ulid.requireValid(quoteId));
    }
}
