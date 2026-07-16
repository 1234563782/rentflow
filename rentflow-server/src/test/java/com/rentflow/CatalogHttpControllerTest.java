package com.rentflow;

import com.rentflow.catalog.api.CatalogQuery;
import com.rentflow.catalog.api.ProductPage;
import com.rentflow.inventory.api.AvailabilityQuery;
import com.rentflow.shared.pagination.PageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogHttpControllerTest {
    @Test
    void bindsBrandAndModelSearchParameters() throws Exception {
        CatalogQuery catalogQuery = mock(CatalogQuery.class);
        AvailabilityQuery availabilityQuery = mock(AvailabilityQuery.class);
        when(catalogQuery.searchProducts(
                null, "laptop", "Apple", "MacBook Pro 14", null, null, null,
                PageQuery.firstPage()
        )).thenReturn(new ProductPage(List.of(), 0, 20, 0, 0));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new CatalogHttpController(catalogQuery, availabilityQuery)
        ).build();

        mockMvc.perform(get("/api/v1/products")
                        .param("equipmentRole", "laptop")
                        .param("brand", "Apple")
                        .param("model", "MacBook Pro 14"))
                .andExpect(status().isOk());

        verify(catalogQuery).searchProducts(
                null, "laptop", "Apple", "MacBook Pro 14", null, null, null,
                PageQuery.firstPage()
        );
    }

    @Test
    void bindsUseCaseSearchParameter() throws Exception {
        CatalogQuery catalogQuery = mock(CatalogQuery.class);
        AvailabilityQuery availabilityQuery = mock(AvailabilityQuery.class);
        String useCaseId = "01J00000000000000000000202";
        when(catalogQuery.searchProducts(
                null, null, null, null, useCaseId, null, null, PageQuery.firstPage()
        )).thenReturn(new ProductPage(List.of(), 0, 20, 0, 0));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new CatalogHttpController(catalogQuery, availabilityQuery)
        ).build();

        mockMvc.perform(get("/api/v1/products").param("useCaseId", useCaseId))
                .andExpect(status().isOk());

        verify(catalogQuery).searchProducts(
                null, null, null, null, useCaseId, null, null, PageQuery.firstPage()
        );
    }
}
