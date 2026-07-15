package com.rentflow.catalog.application;

import com.rentflow.catalog.infrastructure.CatalogMapper;
import com.rentflow.shared.pagination.PageQuery;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CatalogApplicationServiceTest {
    private static final String CATEGORY_ID = "01J00000000000000000000001";

    @Test
    void normalizesBrandAndModelBeforeSearchingAndCounting() {
        CatalogMapper mapper = mock(CatalogMapper.class);
        when(mapper.searchProducts(
                "camera", "camera", "Apple", "MacBook Pro 14", CATEGORY_ID,
                new BigDecimal("500"), 20, 10
        )).thenReturn(List.of());
        CatalogApplicationService service = new CatalogApplicationService(mapper);

        service.searchProducts(
                " camera ", "camera", " Apple ", " MacBook Pro 14 ", CATEGORY_ID,
                new BigDecimal("500"), new PageQuery(2, 10)
        );

        verify(mapper).searchProducts(
                "camera", "camera", "Apple", "MacBook Pro 14", CATEGORY_ID,
                new BigDecimal("500"), 20, 10
        );
        verify(mapper).countProducts(
                "camera", "camera", "Apple", "MacBook Pro 14", CATEGORY_ID,
                new BigDecimal("500")
        );
    }

    @Test
    void treatsBlankBrandAndModelAsAbsent() {
        CatalogMapper mapper = mock(CatalogMapper.class);
        when(mapper.searchProducts(null, null, null, null, null, null, 0, 20))
                .thenReturn(List.of());
        CatalogApplicationService service = new CatalogApplicationService(mapper);

        service.searchProducts(null, null, " ", "\t", null, null, PageQuery.firstPage());

        verify(mapper).searchProducts(null, null, null, null, null, null, 0, 20);
        verify(mapper).countProducts(null, null, null, null, null, null);
    }

    @Test
    void rejectsBrandOrModelLongerThanDatabaseColumns() {
        CatalogMapper mapper = mock(CatalogMapper.class);
        CatalogApplicationService service = new CatalogApplicationService(mapper);
        String tooLong = "x".repeat(65);

        assertThatThrownBy(() -> service.searchProducts(
                null, null, tooLong, null, null, null, PageQuery.firstPage()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("brand must not exceed 64 characters");
        assertThatThrownBy(() -> service.searchProducts(
                null, null, null, tooLong, null, null, PageQuery.firstPage()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("model must not exceed 64 characters");
        verifyNoInteractions(mapper);
    }
}
