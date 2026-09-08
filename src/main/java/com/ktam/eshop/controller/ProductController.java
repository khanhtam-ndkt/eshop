package com.ktam.eshop.controller;

import com.ktam.eshop.api.ProductsApi;
import com.ktam.eshop.model.Product;
import com.ktam.eshop.entity.ProductEntity;
import com.ktam.eshop.service.ProductService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductController implements ProductsApi {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<Product>> getProducts() {
        List<Product> products = service.findAll().stream()
                .map(this::mapToApi)
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @Override
    public ResponseEntity<Void> createProduct(Product product) {
        service.create(product.getName(), product.getPrice());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Product> getProduct(Long id) {
        return ResponseEntity.ok(mapToApi(service.findById(id)));
    }

    @Override
    public ResponseEntity<Void> updateProduct(Long id, Product product) {
        service.update(id, product.getName(), product.getPrice());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteProduct(Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Helper method to keep code clean
    private Product mapToApi(ProductEntity entity) {
        Product p = new Product();
        p.setId(entity.getId());
        p.setName(entity.getName());
        p.setPrice(entity.getPrice());
        return p;
    }
}