package com.cursosdedesarrollo.ejemplospringbootredisdesdecero.config;


import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.model.Product;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Product> productRedisTemplate(ReactiveRedisConnectionFactory factory) {

        // JacksonJsonRedisSerializer apunta a Jackson 3.x (tools.jackson.*), usado en Spring Boot 4.x.
        // El equivalente para Jackson 2.x era Jackson2JsonRedisSerializer.
        JacksonJsonRedisSerializer<Product> valueSerializer = new JacksonJsonRedisSerializer<>(Product.class);

        // Las claves se serializan como cadenas UTF-8 planas; los valores como JSON.
        RedisSerializationContext<String, Product> context =
                RedisSerializationContext.<String, Product>newSerializationContext(new StringRedisSerializer())
                        .value(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }


}
