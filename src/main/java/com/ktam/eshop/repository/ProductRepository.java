package com.ktam.eshop.repository;

import com.ktam.eshop.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    // You get save(), findAll(), findById(), deleteById() for free!
}