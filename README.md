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

## Scripts Docker

| Script | Descripción |
|---|---|
| `docker/00_init.sh` | Inicialización |
| `docker/01_launch.sh` | Levanta Redis y Redis Commander |
| `docker/02_ps.sh` | Lista contenedores activos |
| `docker/03_logs.sh` | Muestra logs |
| `docker/20_destroy.sh` | Para y elimina contenedores |