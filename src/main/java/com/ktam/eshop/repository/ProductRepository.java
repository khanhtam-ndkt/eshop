package com.ktam.eshop.repository;

import com.ktam.eshop.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    // You get save(), findAll(), findById(), deleteById() for free!

    // Deliberately plain JPQL, no index hints. The %name% pattern forces a
    // full scan regardless of any index you add later on name alone — that's
    // the point: this is the query you'll EXPLAIN ANALYZE first, then decide
    // whether a normal index helps (categoryId/price) or you need something
    // else (e.g. a prefix-only search, or a fulltext index) for the name part.
    @Query("""
            SELECT p FROM ProductEntity p
            WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<ProductEntity> search(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);
}
