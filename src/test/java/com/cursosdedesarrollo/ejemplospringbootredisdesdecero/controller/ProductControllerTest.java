package com.cursosdedesarrollo.ejemplospringbootredisdesdecero.controller;


import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.CreateProductRequest;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.ProductResponse;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.UpdateProductRequest;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.exception.ProductNotFoundException;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.handler.GlobalExceptionHandler;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest({ProductController.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProductService productService;

    private ProductResponse sampleResponse() {
        return ProductResponse.builder()
                .id("abc-123")
                .name("Widget")
                .description("A test widget")
                .price(9.99)
                .stock(100)
                .build();
    }

    @Test
    void getAll_returnsListOfProducts() {
        when(productService.findAll()).thenReturn(Flux.just(sampleResponse()));

        webTestClient.get().uri("/api/products")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .hasSize(1);
    }

    @Test
    void getById_existingId_returnsProduct() {
        when(productService.findById("abc-123")).thenReturn(Mono.just(sampleResponse()));

        webTestClient.get().uri("/api/products/abc-123")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("abc-123")
                .jsonPath("$.name").isEqualTo("Widget")
                .jsonPath("$.price").isEqualTo(9.99);
    }

    @Test
    void getById_nonExistingId_returns404() {
        when(productService.findById("no-id")).thenReturn(Mono.error(new ProductNotFoundException("no-id")));

        webTestClient.get().uri("/api/products/no-id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("Product not found with id: no-id");
    }

    @Test
    void create_validRequest_returns201WithLocationHeader() {
        when(productService.create(any(CreateProductRequest.class))).thenReturn(Mono.just(sampleResponse()));

        webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductRequest("Widget", "A test widget", 9.99, 100))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("/api/products/abc-123")
                .expectBody()
                .jsonPath("$.name").isEqualTo("Widget");
    }

    @Test
    void create_invalidRequest_returns400WithFieldErrors() {
        // price negative, stock negative, name blank
        webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductRequest("", null, -1.0, -5))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Validation failed")
                .jsonPath("$.errors").isArray();
    }

    @Test
    void update_existingId_returnsUpdatedProduct() {
        ProductResponse updated = sampleResponse();
        updated.setName("Widget Pro");
        updated.setPrice(19.99);
        when(productService.update(eq("abc-123"), any(UpdateProductRequest.class))).thenReturn(Mono.just(updated));

        webTestClient.put().uri("/api/products/abc-123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateProductRequest("Widget Pro", null, 19.99, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Widget Pro")
                .jsonPath("$.price").isEqualTo(19.99);
    }

    @Test
    void update_nonExistingId_returns404() {
        when(productService.update(eq("no-id"), any(UpdateProductRequest.class)))
                .thenReturn(Mono.error(new ProductNotFoundException("no-id")));

        webTestClient.put().uri("/api/products/no-id")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateProductRequest("Widget Pro", null, null, null))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void delete_existingId_returns204() {
        when(productService.delete("abc-123")).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/products/abc-123")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void delete_nonExistingId_returns404() {
        when(productService.delete("no-id")).thenReturn(Mono.error(new ProductNotFoundException("no-id")));

        webTestClient.delete().uri("/api/products/no-id")
                .exchange()
                .expectStatus().isNotFound();
    }
}
