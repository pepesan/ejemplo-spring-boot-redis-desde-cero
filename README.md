# Ejemplo Spring Boot + Redis desde cero

API REST reactiva de gestión de productos construida con **Spring Boot 4**, **WebFlux** y **Redis** como base de datos principal.

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Framework | Spring Boot 4.0.6 |
| Programación reactiva | Spring WebFlux + Project Reactor |
| Base de datos | Redis (vía `ReactiveRedisTemplate`) |
| Serialización | Jackson JSON (Jackson 3.x) |
| Validación | Jakarta Bean Validation |
| Build | Gradle (Kotlin DSL) |
| Java | 17 |

## Arquitectura

```
Controller (WebFlux)
    └── Service (lógica de negocio, genera UUIDs)
            └── ProductRepository (ReactiveRedisTemplate)
                    └── Redis: claves "product:{uuid}" → JSON
```

Los productos se almacenan en Redis con el patrón de clave `product:{uuid}`. No hay TTL configurado; los datos persisten hasta que se eliminen explícitamente.

## Prerrequisitos

- Docker y Docker Compose
- Java 17+

## Arranque

### 1. Levantar Redis con Docker Compose

```bash
cd docker
bash 00_init.sh
bash 01_launch.sh
```

Esto levanta:
- **Redis** en `localhost:6379`
- **Redis Commander** (UI web) en `http://localhost:8081`

### 2. Arrancar la aplicación

```bash
./gradlew bootRun
```

La API queda disponible en `http://localhost:8080`.

---

## API REST

Base URL: `http://localhost:8080/api/products`

### Endpoints

| Método | Ruta | Descripción | Código éxito |
|---|---|---|---|
| `GET` | `/api/products` | Listar todos los productos | `200 OK` |
| `GET` | `/api/products/{id}` | Obtener producto por ID | `200 OK` |
| `POST` | `/api/products` | Crear producto | `201 Created` |
| `PUT` | `/api/products/{id}` | Actualizar producto (parcial) | `200 OK` |
| `DELETE` | `/api/products/{id}` | Eliminar producto | `204 No Content` |

### Modelo de Producto

```json
{
  "id": "uuid-generado-por-servidor",
  "name": "string (requerido, max 100)",
  "description": "string (opcional, max 500)",
  "price": 9.99,
  "stock": 10
}
```

### Respuesta de error

```json
{
  "timestamp": "2026-05-19T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: abc",
  "path": "/api/products/abc",
  "errors": null
}
```

En errores de validación (`400`), el campo `errors` contiene la lista de campos inválidos:
```json
"errors": [
  { "field": "price", "message": "Price must be positive" }
]
```

---

## Llamadas curl de ejemplo

### Crear productos (guardando el ID)

```bash
# Producto completo — el ID queda en la variable $ID1
ID1=$(curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Teclado mecánico","description":"Switch Cherry MX Red, retroiluminado","price":89.99,"stock":50}' \
  | jq -r '.id')
echo "ID1: $ID1"

# Producto sin descripción — el ID queda en la variable $ID2
ID2=$(curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Ratón inalámbrico","price":29.99,"stock":100}' \
  | jq -r '.id')
echo "ID2: $ID2"
```

### Listar todos los productos

```bash
curl -s http://localhost:8080/api/products | jq
```

### Obtener producto por ID

```bash
curl -s http://localhost:8080/api/products/$ID1 | jq
```

### Actualizar producto (solo los campos enviados se modifican)

```bash
# Actualizar precio y stock
curl -s -X PUT http://localhost:8080/api/products/$ID1 \
  -H "Content-Type: application/json" \
  -d '{"price":79.99,"stock":45}' | jq

# Actualizar solo el nombre
curl -s -X PUT http://localhost:8080/api/products/$ID1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Teclado mecánico Pro"}' | jq
```

### Eliminar producto

```bash
curl -s -X DELETE http://localhost:8080/api/products/$ID2 -o /dev/null -w "%{http_code}\n"
# Espera: 204
```

### Casos de error

```bash
# 404 - Producto inexistente
curl -s http://localhost:8080/api/products/no-existe | jq

# 400 - Validación: falta el precio
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Producto sin precio","stock":10}' | jq

# 400 - Validación: precio negativo
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Producto","price":-5.0,"stock":10}' | jq

# 400 - Validación: stock negativo
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Producto","price":10.0,"stock":-1}' | jq
```

### Script de prueba completo (flujo CRUD)

```bash
#!/bin/bash
BASE="http://localhost:8080/api/products"

echo "=== Crear producto ==="
ID=$(curl -s -X POST $BASE \
  -H "Content-Type: application/json" \
  -d '{"name":"Monitor 4K","description":"27 pulgadas IPS","price":399.99,"stock":20}' \
  | jq -r '.id')
echo "ID creado: $ID"

echo "=== Obtener producto ==="
curl -s $BASE/$ID | jq

echo "=== Actualizar stock ==="
curl -s -X PUT $BASE/$ID \
  -H "Content-Type: application/json" \
  -d '{"stock":15}' | jq

echo "=== Listar todos ==="
curl -s $BASE | jq

echo "=== Eliminar ==="
curl -s -X DELETE $BASE/$ID -o /dev/null -w "HTTP %{http_code}\n"

echo "=== Verificar eliminación (espera 404) ==="
curl -s $BASE/$ID | jq
```

---

## Pruebas automatizadas propuestas

### Estrategia

Se proponen dos niveles de test, alineados con la arquitectura de la aplicación:

```
┌─────────────────────────────────────────────┐
│  Integración (WebTestClient + Testcontainers) │  ProductControllerIntegrationTest
│  → verifica el flujo completo HTTP → Redis    │
├─────────────────────────────────────────────┤
│  Unitario (Mockito)                           │  ProductServiceTest
│  → verifica lógica del servicio aislada       │
└─────────────────────────────────────────────┘
```

### Dependencia necesaria: Testcontainers

Añadir en `build.gradle.kts`:

```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:junit-jupiter")
```

---

### Test de integración: `ProductControllerIntegrationTest`

Levanta un contenedor Redis real con Testcontainers y prueba la API completa con `WebTestClient`.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductControllerIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanRedis() {
        // Limpiar datos entre tests para evitar interferencias
        productRepository.findAll()
                .flatMap(productRepository::delete)
                .blockLast();
    }

    @Test
    void createProduct_returnsCreatedWithLocation() {
        webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {"name":"Teclado","description":"Mecánico","price":89.99,"stock":50}
                    """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", "/api/products/.+")
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Teclado")
                .jsonPath("$.price").isEqualTo(89.99)
                .jsonPath("$.stock").isEqualTo(50);
    }

    @Test
    void getById_existingProduct_returnsOk() {
        // Given
        String id = createProductAndGetId("Ratón", 29.99, 100);

        // Then
        webTestClient.get().uri("/api/products/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id)
                .jsonPath("$.name").isEqualTo("Ratón");
    }

    @Test
    void getById_nonExistingProduct_returns404() {
        webTestClient.get().uri("/api/products/no-existe")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").value(Matchers.containsString("no-existe"));
    }

    @Test
    void getAll_returnsAllProducts() {
        createProductAndGetId("Producto A", 10.0, 5);
        createProductAndGetId("Producto B", 20.0, 10);

        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .hasSize(2);
    }

    @Test
    void updateProduct_partialUpdate_onlyChangesSuppliedFields() {
        String id = createProductAndGetId("Monitor", 399.99, 20);

        webTestClient.put().uri("/api/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"stock":15}""")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.stock").isEqualTo(15)
                .jsonPath("$.name").isEqualTo("Monitor")       // sin cambios
                .jsonPath("$.price").isEqualTo(399.99);        // sin cambios
    }

    @Test
    void deleteProduct_existingProduct_returns204() {
        String id = createProductAndGetId("Auriculares", 59.99, 30);

        webTestClient.delete().uri("/api/products/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        // Verificar que ya no existe
        webTestClient.get().uri("/api/products/{id}", id)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createProduct_missingRequiredField_returns400WithFieldErrors() {
        webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"name":"Sin precio","stock":10}""")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[?(@.field=='price')].message")
                .isEqualTo("Price is required");
    }

    @Test
    void createProduct_negativePrice_returns400() {
        webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"name":"Malo","price":-1.0,"stock":10}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    private String createProductAndGetId(String name, double price, int stock) {
        return webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(String.format(
                    """{"name":"%s","price":%s,"stock":%d}""", name, price, stock))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(ProductResponse.class)
                .getResponseBody()
                .blockFirst()
                .getId();
    }
}
```

---

### Test unitario: `ProductServiceTest`

Prueba la lógica del servicio en aislamiento, mockeando el repositorio con Mockito.

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void findById_existingId_returnsProductResponse() {
        Product product = Product.builder()
                .id("abc-123").name("Teclado").price(89.99).stock(50).build();
        when(productRepository.findById("abc-123")).thenReturn(Mono.just(product));

        StepVerifier.create(productService.findById("abc-123"))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("abc-123");
                    assertThat(response.getName()).isEqualTo("Teclado");
                    assertThat(response.getPrice()).isEqualTo(89.99);
                })
                .verifyComplete();
    }

    @Test
    void findById_nonExistingId_throwsProductNotFoundException() {
        when(productRepository.findById("no-existe")).thenReturn(Mono.empty());

        StepVerifier.create(productService.findById("no-existe"))
                .expectErrorMatches(e ->
                    e instanceof ProductNotFoundException &&
                    e.getMessage().contains("no-existe"))
                .verify();
    }

    @Test
    void create_assignsUuidAndPersists() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Ratón").price(29.99).stock(100).build();

        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(productService.create(request))
                .assertNext(response -> {
                    assertThat(response.getId()).isNotNull();
                    assertThat(response.getName()).isEqualTo("Ratón");
                })
                .verifyComplete();

        verify(productRepository).save(argThat(p ->
            p.getId() != null && p.getName().equals("Ratón")));
    }

    @Test
    void update_existingProduct_onlyModifiesNonNullFields() {
        Product existing = Product.builder()
                .id("abc").name("Monitor").description("27 pulgadas").price(399.99).stock(20).build();
        UpdateProductRequest request = UpdateProductRequest.builder().stock(15).build();

        when(productRepository.findById("abc")).thenReturn(Mono.just(existing));
        when(productRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(productService.update("abc", request))
                .assertNext(response -> {
                    assertThat(response.getStock()).isEqualTo(15);
                    assertThat(response.getName()).isEqualTo("Monitor");   // no cambia
                    assertThat(response.getPrice()).isEqualTo(399.99);    // no cambia
                })
                .verifyComplete();
    }

    @Test
    void update_nonExistingProduct_throwsProductNotFoundException() {
        when(productRepository.findById("no-existe")).thenReturn(Mono.empty());

        StepVerifier.create(productService.update("no-existe", new UpdateProductRequest()))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void delete_existingProduct_completesSuccessfully() {
        Product product = Product.builder().id("abc").name("Monitor").price(399.99).stock(20).build();
        when(productRepository.findById("abc")).thenReturn(Mono.just(product));
        when(productRepository.delete(product)).thenReturn(Mono.empty());

        StepVerifier.create(productService.delete("abc"))
                .verifyComplete();

        verify(productRepository).delete(product);
    }

    @Test
    void delete_nonExistingProduct_throwsProductNotFoundException() {
        when(productRepository.findById("no-existe")).thenReturn(Mono.empty());

        StepVerifier.create(productService.delete("no-existe"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }
}
```

---

## Scripts Docker

| Script | Descripción |
|---|---|
| `docker/00_init.sh` | Inicialización |
| `docker/01_launch.sh` | Levanta Redis y Redis Commander |
| `docker/02_ps.sh` | Lista contenedores activos |
| `docker/03_logs.sh` | Muestra logs |
| `docker/20_destroy.sh` | Para y elimina contenedores |