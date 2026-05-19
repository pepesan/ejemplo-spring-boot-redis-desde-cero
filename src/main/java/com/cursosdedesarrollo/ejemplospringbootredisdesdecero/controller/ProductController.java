package com.cursosdedesarrollo.ejemplospringbootredisdesdecero.controller;


import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.CreateProductRequest;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.ProductResponse;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.UpdateProductRequest;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Flux<ProductResponse> getAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ProductResponse> getById(@PathVariable String id) {
        return productService.findById(id);
    }

    @PostMapping
    public Mono<ResponseEntity<ProductResponse>> create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request)
                .map(product -> ResponseEntity
                        .created(URI.create("/api/products/" + product.getId()))
                        .body(product));
    }

    @PutMapping("/{id}")
    public Mono<ProductResponse> update(@PathVariable String id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return productService.delete(id)
                .then(Mono.just(ResponseEntity.<Void>noContent().build()));
    }
}
