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

    List<ProductRow> searchProducts(
            @Param("keyword") String keyword,
            @Param("equipmentRole") String equipmentRole,
            @Param("categoryId") String categoryId,
            @Param("maxDailyRate") BigDecimal maxDailyRate,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long countProducts(
            @Param("keyword") String keyword,
            @Param("equipmentRole") String equipmentRole,
            @Param("categoryId") String categoryId,
            @Param("maxDailyRate") BigDecimal maxDailyRate
    );

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
