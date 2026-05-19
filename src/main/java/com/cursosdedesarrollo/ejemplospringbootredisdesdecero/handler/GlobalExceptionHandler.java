package com.cursosdedesarrollo.ejemplospringbootredisdesdecero.handler;


import com.cursosdedesarrollo.ejemplospringbootredisdesdecero.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponse> handleProductNotFound(ProductNotFoundException ex, ServerWebExchange exchange) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, null);
    }



    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", exchange, fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error at {}", exchange.getRequest().getPath().value(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", exchange, null);
    }

    private Mono<ErrorResponse> buildError(HttpStatus status, String message, ServerWebExchange exchange,
                                            List<ErrorResponse.FieldError> errors) {
        return Mono.just(ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(exchange.getRequest().getPath().value())
                .errors(errors)
                .build());
    }
}
