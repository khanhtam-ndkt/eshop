package com.ktam.eshop.controller;

import com.ktam.eshop.entity.ProductEntity;
import com.ktam.eshop.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Not part of api.yml on purpose — this is the load-test / bottleneck-analysis
 * target, kept separate from the "real" generated API so it's obvious which
 * endpoint you're hammering with JMeter/k6 and which is the app's real
 * contract.
 *
 * GET /api/products/search?name=phone&categoryId=3&minPrice=10&maxPrice=500&page=0&size=20
 */
@RestController
public class ProductSearchController {

    private final ProductRepository repository;

    public ProductSearchController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/products/search")
    public Page<ProductEntity> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return repository.search(name, categoryId, minPrice, maxPrice, pageable);
    }
}
