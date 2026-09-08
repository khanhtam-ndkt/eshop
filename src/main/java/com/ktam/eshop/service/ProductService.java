package com.ktam.eshop.service;

import com.ktam.eshop.entity.ProductEntity;
import com.ktam.eshop.exception.ProductNotFoundException;
import com.ktam.eshop.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Single place for product business logic. ProductController (REST) and
 * MainView (Vaadin) both call this instead of talking to ProductRepository
 * directly, so create/update/delete/lookup rules live in one spot.
 */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductEntity> findAll() {
        return repository.findAll();
    }

    public ProductEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public ProductEntity create(String name, Double price) {
        ProductEntity entity = new ProductEntity();
        entity.setName(name);
        entity.setPrice(price);
        return repository.save(entity);
    }

    public ProductEntity update(Long id, String name, Double price) {
        ProductEntity entity = findById(id);
        entity.setName(name);
        entity.setPrice(price);
        return repository.save(entity);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
