package com.rentflow.catalog.application;

import com.rentflow.catalog.infrastructure.CatalogMapper;
import com.rentflow.catalog.infrastructure.ProductRow;
import com.rentflow.catalog.infrastructure.ProductUseCaseRow;
import com.rentflow.catalog.infrastructure.UseCaseAliasRow;
import com.rentflow.catalog.infrastructure.UseCaseRow;
import com.rentflow.shared.pagination.PageQuery;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CatalogApplicationServiceTest {
    private static final String CATEGORY_ID = "01J00000000000000000000001";
    private static final String PRODUCT_ID = "01J00000000000000000000105";
    private static final String USE_CASE_ID = "01J00000000000000000000202";

    @Test
    void returnsDynamicUseCasesWithDatabaseAliases() {
        CatalogMapper mapper = mock(CatalogMapper.class);
        when(mapper.listUseCases()).thenReturn(List.of(
                new UseCaseRow(USE_CASE_ID, "video_editing", "视频剪辑", "视频后期制作")
        ));
        when(mapper.listUseCaseAliases()).thenReturn(List.of(
                new UseCaseAliasRow(USE_CASE_ID, "剪辑"),
                new UseCaseAliasRow(USE_CASE_ID, "后期")
        ));

        var result = new CatalogApplicationService(mapper).listUseCases();

        assertThat(result).singleElement().satisfies(useCase -> {
            assertThat(useCase.code()).isEqualTo("video_editing");
            assertThat(useCase.aliases()).containsExactly("剪辑", "后期");
        });
    }

    @Test
    void filtersAndHydratesProductsByDynamicUseCase() {
        CatalogMapper mapper = mock(CatalogMapper.class);
        ProductRow product = new ProductRow(
                PRODUCT_ID, CATEGORY_ID, "laptop", "MacBook Pro 14", "Apple",
                "MacBook Pro 14", "移动剪辑工作站"
        );
        when(mapper.searchProducts(
                null, "laptop", null, null, USE_CASE_ID, null, null, 0, 20
        )).thenReturn(List.of(product));
        when(mapper.listProductUseCases(List.of(PRODUCT_ID))).thenReturn(List.of(
                new ProductUseCaseRow(
                        PRODUCT_ID, USE_CASE_ID, "video_editing", "视频剪辑",
                        new BigDecimal("0.98")
                )
        ));
        CatalogApplicationService service = new CatalogApplicationService(mapper);

        var result = service.searchProducts(
                null, "laptop", null, null, USE_CASE_ID, null, null,
                PageQuery.firstPage()
        );

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(PRODUCT_ID);
            assertThat(item.useCases()).singleElement().satisfies(useCase ->
                    assertThat(useCase.id()).isEqualTo(USE_CASE_ID)
            );
        });
        verify(mapper).countProducts(
                null, "laptop", null, null, USE_CASE_ID, null, null
        );
    }

    @Test
    void normalizesBrandAndModelBeforeSearchingAndCounting() {
        CatalogMapper mapper = mock(CatalogMapper.class);
        when(mapper.searchProducts(
                "camera", "camera", "Apple", "MacBook Pro 14", null, CATEGORY_ID,
                new BigDecimal("500"), 20, 10
        )).thenReturn(List.of());
        CatalogApplicationService service = new CatalogApplicationService(mapper);

        service.searchProducts(
                " camera ", "camera", " Apple ", " MacBook Pro 14 ", null, CATEGORY_ID,
                new BigDecimal("500"), new PageQuery(2, 10)
        );

        verify(mapper).searchProducts(
                "camera", "camera", "Apple", "MacBook Pro 14", null, CATEGORY_ID,
                new BigDecimal("500"), 20, 10
        );
        verify(mapper).countProducts(
                "camera", "camera", "Apple", "MacBook Pro 14", null, CATEGORY_ID,
                new BigDecimal("500")
        );
    }

    @Test
    void treatsBlankBrandAndModelAsAbsent() {
        CatalogMapper mapper = mock(CatalogMapper.class);
        when(mapper.searchProducts(null, null, null, null, null, null, null, 0, 20))
                .thenReturn(List.of());
        CatalogApplicationService service = new CatalogApplicationService(mapper);

        service.searchProducts(null, null, " ", "\t", null, null, null, PageQuery.firstPage());

        verify(mapper).searchProducts(null, null, null, null, null, null, null, 0, 20);
        verify(mapper).countProducts(null, null, null, null, null, null, null);
    }

    @Test
    void rejectsBrandOrModelLongerThanDatabaseColumns() {
        CatalogMapper mapper = mock(CatalogMapper.class);
        CatalogApplicationService service = new CatalogApplicationService(mapper);
        String tooLong = "x".repeat(65);

        assertThatThrownBy(() -> service.searchProducts(
                null, null, tooLong, null, null, null, null, PageQuery.firstPage()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("brand must not exceed 64 characters");
        assertThatThrownBy(() -> service.searchProducts(
                null, null, null, tooLong, null, null, null, PageQuery.firstPage()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("model must not exceed 64 characters");
        verifyNoInteractions(mapper);
    }
}
