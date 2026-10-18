package com.mercadosolidario.repository;

import com.mercadosolidario.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
    List<Product> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByActiveTrue();

    long countByActiveTrue();

    long countByActiveFalse();

    long countByActiveTrueAndCurrentQuantityLessThanEqual(Integer quantity);

    List<Product> findByActiveTrueOrderByCurrentQuantityAsc();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.currentQuantity <= p.minimumQuantity")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.currentQuantity <= p.minimumQuantity ORDER BY p.currentQuantity ASC")
    List<Product> findLowStockProducts(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.currentQuantity <= p.minimumQuantity")
    long countLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.category = :category")
    List<Product> findByCategory(String category);

    @Query("SELECT p FROM Product p WHERE p.currentQuantity < :minimum")
    List<Product> findByCurrentQuantityLessThan(Integer minimum);
}
