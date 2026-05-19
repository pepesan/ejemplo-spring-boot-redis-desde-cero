package com.cursosdedesarrollo.ejemplospringbootredisdesdecero.service;

import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.CreateProductRequest;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.ProductResponse;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.dto.UpdateProductRequest;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.exception.ProductNotFoundException;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.model.Product;
import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

// No se necesita Schedulers.boundedElastic(): Lettuce (el driver de Redis) es completamente no bloqueante,
// por lo que las operaciones Redis nunca bloquean el event loop — al contrario que JPA, que sí lo requiere.
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Flux<ProductResponse> findAll() {
        return productRepository.findAll()
                .map(this::toResponse);
    }

    public Mono<ProductResponse> findById(String id) {
        // switchIfEmpty es el equivalente reactivo de Optional.orElseThrow() usado en los servicios JPA.
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .map(this::toResponse);
    }

    public Mono<ProductResponse> create(CreateProductRequest request) {
        // Redis no tiene @GeneratedValue — la aplicación es responsable de generar el ID.
        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .build();
        return productRepository.save(product)
                .map(this::toResponse);
    }

    public Mono<ProductResponse> update(String id, UpdateProductRequest request) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .flatMap(product -> {
                    if (request.getName() != null) product.setName(request.getName());
                    if (request.getDescription() != null) product.setDescription(request.getDescription());
                    if (request.getPrice() != null) product.setPrice(request.getPrice());
                    if (request.getStock() != null) product.setStock(request.getStock());
                    return productRepository.save(product);
                })
                .map(this::toResponse);
    }

    public Mono<Void> delete(String id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .flatMap(productRepository::delete);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }
}
