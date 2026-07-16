package com.rentflow.catalog.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Mapper
public interface CatalogMapper {
    @Select("""
            SELECT id, name, sort_order
            FROM categories
            WHERE enabled = TRUE
            ORDER BY sort_order ASC, id ASC
            """)
    List<CategoryRow> listCategories();

    @Select("""
            SELECT id, code, name, description
            FROM catalog_use_cases
            WHERE enabled = TRUE
            ORDER BY name ASC, id ASC
            """)
    List<UseCaseRow> listUseCases();

    @Select("""
            SELECT use_case_id, alias
            FROM catalog_use_case_aliases
            ORDER BY use_case_id ASC, alias ASC
            """)
    List<UseCaseAliasRow> listUseCaseAliases();

    List<ProductRow> searchProducts(
            @Param("keyword") String keyword,
            @Param("equipmentRole") String equipmentRole,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("useCaseId") String useCaseId,
            @Param("categoryId") String categoryId,
            @Param("maxDailyRate") BigDecimal maxDailyRate,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long countProducts(
            @Param("keyword") String keyword,
            @Param("equipmentRole") String equipmentRole,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("useCaseId") String useCaseId,
            @Param("categoryId") String categoryId,
            @Param("maxDailyRate") BigDecimal maxDailyRate
    );

    List<ProductUseCaseRow> listProductUseCases(@Param("productIds") List<String> productIds);

    @Select("""
            SELECT id, category_id, equipment_role, name, brand, model, description,
                   daily_rate, fixed_deposit, pricing_version
            FROM products
            WHERE id = #{productId} AND enabled = TRUE
            """)
    Optional<ProductRow> findEnabledProduct(@Param("productId") String productId);

    @Select("""
            SELECT id, category_id, equipment_role, name, brand, model, description,
                   daily_rate, fixed_deposit, pricing_version
            FROM products
            WHERE id = #{productId}
            """)
    Optional<ProductRow> findProduct(@Param("productId") String productId);
}
