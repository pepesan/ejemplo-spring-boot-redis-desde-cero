package com.cursosdedesarrollo.ejemplospringbootredisdesdecero.repository;

import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

// A diferencia de JPA, Spring Data Redis no soporta ReactiveCrudRepository.
// Todo el acceso a Redis se hace directamente a través de ReactiveRedisTemplate.
@Repository
@RequiredArgsConstructor
public class ProductRepository {

    // Redis tiene un espacio de claves plano. El prefijo agrupa todas las claves de producto: "product:{uuid}".
    private static final String KEY_PREFIX = "product:";

    private final ReactiveRedisTemplate<String, Product> redisTemplate;

    public Flux<Product> findAll() {
        // KEYS es O(N) — válido para desarrollo, pero en producción con muchas claves usar SCAN.
        return redisTemplate.keys(KEY_PREFIX + "*")
                .flatMap(key -> redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull); // get() emite vacío si la clave expiró entre KEYS y GET
    }

    public Mono<Product> findById(String id) {
        // opsForValue() opera sobre el tipo String de Redis (clave → valor serializado).
        // Devuelve Mono.empty() si la clave no existe; la capa de servicio lo convierte en 404.
        return redisTemplate.opsForValue().get(KEY_PREFIX + id);
    }

    public Mono<Product> save(Product product) {
        // SET devuelve Mono<Boolean> (flag de éxito), no la entidad guardada.
        // thenReturn() descarta ese booleano y re-emite el objeto original, igualando el contrato de JPA save().
        return redisTemplate.opsForValue()
                .set(KEY_PREFIX + product.getId(), product)
                .thenReturn(product);
    }

    public Mono<Void> delete(Product product) {
        return redisTemplate.delete(KEY_PREFIX + product.getId()).then();
    }
}
