package com.ktam.eshop.controller;

import com.ktam.eshop.api.ProductsApi;
import com.ktam.eshop.model.Product;
import com.ktam.eshop.entity.ProductEntity;
import com.ktam.eshop.exception.ProductNotFoundException;
import com.ktam.eshop.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductController implements ProductsApi {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public ResponseEntity<List<Product>> getProducts() {
        List<ProductEntity> entities = repository.findAll();
        List<Product> products = entities.stream().map(this::mapToApi).collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @Override
    public ResponseEntity<Void> createProduct(Product product) {
        ProductEntity newEntity = new ProductEntity();
        newEntity.setName(product.getName());
        newEntity.setPrice(product.getPrice());
        repository.save(newEntity);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Product> getProduct(Long id) {
        ProductEntity entity = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ResponseEntity.ok(mapToApi(entity));
    }

    @Override
    public ResponseEntity<Void> updateProduct(Long id, Product product) {
        ProductEntity entityToUpdate = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        entityToUpdate.setName(product.getName());
        entityToUpdate.setPrice(product.getPrice());
        repository.save(entityToUpdate);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteProduct(Long id) {
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
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