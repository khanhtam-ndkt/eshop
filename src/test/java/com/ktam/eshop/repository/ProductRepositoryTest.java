package com.ktam.eshop.repository;

import com.ktam.eshop.entity.ProductEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Test
    public void testSaveAndRetrieveProduct() {
        // 1. Create a new product entity
        ProductEntity product = new ProductEntity();
        product.setName("Gaming Mouse");
        product.setPrice(59.99);

        // 2. Save it to the database
        repository.save(product);

        // 3. Retrieve all products
        List<ProductEntity> products = repository.findAll();

        // 4. Assert that the database contains our saved product
        assertThat(products).isNotEmpty();
        assertThat(products.stream().anyMatch(p -> p.getName().equals("Gaming Mouse"))).isTrue();
    }
}