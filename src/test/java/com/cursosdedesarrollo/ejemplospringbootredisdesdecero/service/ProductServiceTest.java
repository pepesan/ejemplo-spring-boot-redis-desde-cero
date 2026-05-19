package com.cursosdedesarrollo.ejemplospringbootredisdesdecero.service;


import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.CreateProductRequest;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.UpdateProductRequest;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.exception.ProductNotFoundException;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.model.Product;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct() {
        return Product.builder()
                .id("abc-123")
                .name("Widget")
                .description("A test widget")
                .price(9.99)
                .stock(100)
                .build();
    }

    @Test
    void findAll_returnsAllProducts() {
        when(productRepository.findAll()).thenReturn(Flux.just(sampleProduct()));

        StepVerifier.create(productService.findAll())
                .assertNext(p -> assertThat(p.getName()).isEqualTo("Widget"))
                .verifyComplete();
    }

    @Test
    void findById_existingId_returnsProduct() {
        when(productRepository.findById("abc-123")).thenReturn(Mono.just(sampleProduct()));

        StepVerifier.create(productService.findById("abc-123"))
                .assertNext(p -> {
                    assertThat(p.getId()).isEqualTo("abc-123");
                    assertThat(p.getPrice()).isEqualTo(9.99);
                })
                .verifyComplete();
    }

    @Test
    void findById_nonExistingId_throwsProductNotFoundException() {
        when(productRepository.findById("no-id")).thenReturn(Mono.empty());

        StepVerifier.create(productService.findById("no-id"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void create_validRequest_assignsIdAndSaves() {
        CreateProductRequest request = new CreateProductRequest("Widget", "A test widget", 9.99, 100);
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(sampleProduct()));

        StepVerifier.create(productService.create(request))
                .assertNext(p -> {
                    assertThat(p.getName()).isEqualTo("Widget");
                    assertThat(p.getPrice()).isEqualTo(9.99);
                })
                .verifyComplete();

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_existingProduct_updatesFields() {
        UpdateProductRequest request = new UpdateProductRequest("Widget Pro", null, 19.99, null);
        Product updated = sampleProduct();
        updated.setName("Widget Pro");
        updated.setPrice(19.99);

        when(productRepository.findById("abc-123")).thenReturn(Mono.just(sampleProduct()));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(productService.update("abc-123", request))
                .assertNext(p -> {
                    assertThat(p.getName()).isEqualTo("Widget Pro");
                    assertThat(p.getPrice()).isEqualTo(19.99);
                })
                .verifyComplete();
    }

    @Test
    void update_nonExistingId_throwsProductNotFoundException() {
        when(productRepository.findById("no-id")).thenReturn(Mono.empty());

        StepVerifier.create(productService.update("no-id", new UpdateProductRequest()))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void delete_existingId_deletesProduct() {
        when(productRepository.findById("abc-123")).thenReturn(Mono.just(sampleProduct()));
        when(productRepository.delete(any(Product.class))).thenReturn(Mono.empty());

        StepVerifier.create(productService.delete("abc-123"))
                .verifyComplete();

        verify(productRepository).delete(any(Product.class));
    }

    @Test
    void delete_nonExistingId_throwsProductNotFoundException() {
        when(productRepository.findById("no-id")).thenReturn(Mono.empty());

        StepVerifier.create(productService.delete("no-id"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }
}
