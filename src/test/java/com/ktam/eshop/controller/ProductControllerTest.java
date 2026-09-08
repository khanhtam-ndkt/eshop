package com.ktam.eshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktam.eshop.entity.ProductEntity;
import com.ktam.eshop.exception.ProductNotFoundException;
import com.ktam.eshop.model.Product;
import com.ktam.eshop.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer slice tests for ProductController.
 *
 * NOTE: the two imports below are Spring Boot 4 / Spring Framework 7 package
 * locations, following the same relocation pattern already used by the
 * existing DataJpaTest (org.springframework.boot.data.jpa.test.autoconfigure.*).
 * If your exact dependency versions differ, adjust these two imports:
 *   - org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
 *   - org.springframework.test.context.bean.override.mockito.MockitoBean
 * (older Spring Boot 3.x: org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
 *  and org.springframework.boot.test.mock.mockito.MockBean)
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getProducts_returnsListOfProducts() throws Exception {
        ProductEntity entity = new ProductEntity("Gaming Mouse", 59.99);
        entity.setId(1L);
        when(service.findAll()).thenReturn(List.of(entity));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Gaming Mouse"))
                .andExpect(jsonPath("$[0].price").value(59.99));
    }

    @Test
    void getProduct_whenFound_returns200() throws Exception {
        ProductEntity entity = new ProductEntity("Keyboard", 89.0);
        entity.setId(1L);
        when(service.findById(1L)).thenReturn(entity);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void getProduct_whenMissing_returns404WithErrorBody() throws Exception {
        when(service.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    void createProduct_whenValid_returns201() throws Exception {
        Product product = new Product();
        product.setName("Monitor");
        product.setPrice(199.99);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated());

        verify(service, times(1)).create("Monitor", 199.99);
    }

    @Test
    void createProduct_whenNameBlank_returns400WithFieldError() throws Exception {
        Product product = new Product();
        product.setName("");
        product.setPrice(10.0);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());

        verify(service, never()).create(anyString(), anyDouble());
    }

    @Test
    void createProduct_whenPriceNotPositive_returns400() throws Exception {
        Product product = new Product();
        product.setName("Webcam");
        product.setPrice(0.0);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'price')]").exists());
    }

    @Test
    void updateProduct_whenFound_returns200() throws Exception {
        ProductEntity updated = new ProductEntity("New Name", 20.0);
        updated.setId(1L);
        when(service.update(eq(1L), anyString(), anyDouble())).thenReturn(updated);

        Product update = new Product();
        update.setName("New Name");
        update.setPrice(20.0);

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        verify(service, times(1)).update(1L, "New Name", 20.0);
    }

    @Test
    void updateProduct_whenMissing_returns404() throws Exception {
        when(service.update(eq(99L), anyString(), anyDouble()))
                .thenThrow(new ProductNotFoundException(99L));

        Product update = new Product();
        update.setName("Doesn't matter");
        update.setPrice(20.0);

        mockMvc.perform(put("/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_whenFound_returns204() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNoContent());

        verify(service, times(1)).delete(1L);
    }

    @Test
    void deleteProduct_whenMissing_returns404() throws Exception {
        doThrow(new ProductNotFoundException(99L)).when(service).delete(99L);

        mockMvc.perform(delete("/products/99"))
                .andExpect(status().isNotFound());
    }
}