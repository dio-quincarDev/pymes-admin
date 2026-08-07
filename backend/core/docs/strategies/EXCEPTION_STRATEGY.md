# Exception Strategy — Core Service

Estandarizacion del manejo de excepciones en el microservicio core, replicando el patron de auth.

## Arquitectura

```
common/exception/
├── ApiResponse.java              ← Envoltura estandar success/error
├── ErrorResponse.java            ← Record de error con codigo + mensaje + path + timestamp + detalles
├── CodigoError.java              ← Enum de codigos de error por dominio
├── CoreApiException.java         ← Clase base abstracta (extiende RuntimeException)
├── GlobalExceptionHandler.java   ← @RestControllerAdvice con handlers por tipo
└── custom/
    ├── ResourceNotFoundException.java   ← 404
    ├── DuplicateResourceException.java  ← 409
    └── InvalidInputException.java       ← 400
```

## ErrorResponse

Record estandar para toda respuesta de error:

```java
public record ErrorResponse(
    String codigo,
    String mensaje,
    String path,
    String timestamp,    // ISO-8601
    Map<String, String> detalles   // field errors, null si no aplica
)
```

Constructores convenience: `(codigo, mensaje, path)` sin detalles, y `(codigo, mensaje, path, detalles)`.

## ApiResponse

Wrapper unico para todas las respuestas HTTP:

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorResponse error
) {
    static <T> ApiResponse<T> ok(T data)       // success=true, data=payload
    static <T> ApiResponse<T> ok()             // success=true, data=null
    static <T> ApiResponse<T> error(ErrorResponse e)  // success=false, error=e
}
```

Todos los endpoints existentes que devuelven `ResponseEntity<XxxResponse>` deben migrar a `ResponseEntity<ApiResponse<XxxResponse>>`.

## CodigoError

Enum con codigos alfanumericos categorizados por modulo. Formato: `XXXNNN` (3 letras de modulo + 3 digitos).

| Categoria | Prefijo | Ejemplos |
|-----------|---------|----------|
| Setup / Onboarding | SETUP | SETUP001 (industry not found), SETUP002 (onboarding incompleto) |
| Productos | PROD | PROD001 (producto no encontrado), PROD002 (presentacion no pertenece al tenant) |
| Facturas / Proveedores | INV | INV001 (factura no encontrada), INV002 (proveedor no encontrado) |
| Accounting | ACCTG | ACCTG001 (metricas no disponibles) |
| Gastos | EXP | EXP001 (gasto no encontrado) |
| Ventas | VENT | VENT001 (venta no encontrada) |
| Prestamos | LOAN | LOAN001 (prestamo no encontrado) |
| Patrimonio | PATR | PATR001 (patrimonio no encontrado) |
| Validacion | VAL | VAL001 (validation error), VAL002 (invalid input), VAL003 (constraint violation) |
| Recurso | RSC | RSC001 (not found), RSC002 (method not allowed) |
| Internal | ERR | ERR999 (error interno) |

Cada constante tiene:
- `codigo`: string (ej: "PROD001")
- `mensaje`: template con soporte `MessageFormat` (ej: "Producto {0} no encontrado")
- `httpStatus`: HttpStatus asociado

## CoreApiException

Clase base abstracta que toda excepcion del core debe extender:

```java
public abstract class CoreApiException extends RuntimeException {
    String getCodigo()
    CodigoError getCodigoError()
    HttpStatus getHttpStatus()
}
```

Constructores: `(CodigoError)`, `(CodigoError, String mensaje)`, `(CodigoError, Object... params)`, `(CodigoError, Throwable cause)`, `(CodigoError, String mensaje, Throwable cause)`.

El constructor con `params` formatea el mensaje del enum con `MessageFormat` (ej: `new ResourceNotFoundException(PROD001, id)` produce "Producto xxx no encontrado").

## Excepciones Custom

### ResourceNotFoundException (404)

```java
new ResourceNotFoundException(CodigoError.PROD001, productoId)
new ResourceNotFoundException(CodigoError.INV001, facturaId)
```

Se usa cuando una entidad no existe. Reemplaza `EntityNotFoundException` y `IllegalArgumentException` con mensaje "not found".

### DuplicateResourceException (409)

```java
new DuplicateResourceException(CodigoError.VAL004, "Slug already taken")
```

Se usa cuando un recurso duplicado impide la operacion (unique constraint).

### InvalidInputException (400)

```java
new InvalidInputException(CodigoError.VAL002, mensaje)
```

Se usa para validaciones de negocio que no cubre `@Valid`. Reemplaza `IllegalArgumentException` en casos de entrada invalida.

## GlobalExceptionHandler

Handlers a implementar (orden de precedencia):

| Prioridad | Excepcion | HTTP | Comportamiento |
|-----------|-----------|------|----------------|
| 1 | `CoreApiException` | segun CodigoError | log warn, extrae codigo/mensaje/path de la excepcion |
| 2 | `MethodArgumentNotValidException` | 400 | construye `detalles` con field errors, log warn |
| 3 | `HttpMessageNotReadableException` | 400 | JSON malformado |
| 4 | `MissingServletRequestParameterException` | 400 | parametro requerido faltante |
| 5 | `ConstraintViolationException` | 409 | unique/foreign key violations |
| 6 | `DataIntegrityViolationException` | 409 | detecta unique vs foreign key |
| 7 | `AccessDeniedException` | 403 | permisos insuficientes |
| 8 | `EntityNotFoundException` | 404 | compatibilidad con codigo legacy |
| 9 | `IllegalArgumentException` | 400 | compatibilidad con codigo legacy |
| 10 | `Exception` (catch-all) | 500 | log error con stacktrace |

El handler de `CoreApiException` es el principal: todas las excepciones custom heredan de el y se resuelven con una unica entrada.

## Migracion

Los 17 throws existentes se migran segun esta tabla:

| Ubicacion | Throw actual | Reemplazar por |
|-----------|-------------|----------------|
| `SetupServiceImpl.java:41` | `IllegalArgumentException("Industry not found")` | `ResourceNotFoundException(SETUP001, industry)` |
| `SetupServiceImpl.java:164` | `IllegalArgumentException("Industry not found")` | `ResourceNotFoundException(SETUP001, industry)` |
| `ProductoServiceImpl.java:163` | `IllegalArgumentException("Presentacion no pertenece")` | `InvalidInputException(PROD002, presentacionId, tenantId)` |
| `ProductoServiceImpl.java:173` | `EntityNotFoundException("Producto not found")` | `ResourceNotFoundException(PROD001, id)` |
| `FacturaServiceImpl.java:177` | `IllegalStateException("Solo facturas REGISTRADA")` | `InvalidInputException(INV008, ...)` |
| `FacturaServiceImpl.java:241` | `EntityNotFoundException("Presentacion not found")` | `ResourceNotFoundException(PROD003, presentacionId)` |
| `FacturaServiceImpl.java:244` | `IllegalArgumentException("Presentacion no pertenece")` | `InvalidInputException(PROD002, ...)` |
| `FacturaServiceImpl.java:327` | `IllegalStateException("Factura already ...")` | `InvalidInputException(INV005, status)` |
| `FacturaServiceImpl.java:340` | `IllegalStateException("Cannot delete ...")` | `InvalidInputException(INV006, status)` |
| `FacturaServiceImpl.java:352` | `EntityNotFoundException("Proveedor not found")` | `ResourceNotFoundException(INV002, id)` |
| `FacturaServiceImpl.java:361` | `EntityNotFoundException("Factura not found")` | `ResourceNotFoundException(INV001, id)` |
| `GastoServiceImpl.java:90` | `EntityNotFoundException("Gasto not found")` | `ResourceNotFoundException(EXP001, id)` |
| `VentaServiceImpl.java:85` | `EntityNotFoundException("Venta not found")` | `ResourceNotFoundException(VENT001, id)` |
| `PrestamoServiceImpl.java:133` | `EntityNotFoundException("Prestamo not found")` | `ResourceNotFoundException(LOAN001, id)` |
| `InvoiceCalculator.java:80` | `IllegalArgumentException("2 inputs requeridos")` | `InvalidInputException(INV007, ...)` |
| `InvoiceCalculator.java:88` | `IllegalArgumentException("No se puede resolver cantidad")` | `InvalidInputException(INV007, ...)` |
| `InvoiceCalculator.java:95` | `IllegalArgumentException("No se puede resolver precio")` | `InvalidInputException(INV007, ...)` |

## Response entity en controllers

Los controllers deben cambiar el return type de:

```java
ResponseEntity<List<ProductoResponse>> findAll(@RequestParam UUID tenantId)
```

a:

```java
ResponseEntity<ApiResponse<List<ProductoResponse>>> findAll(@RequestParam UUID tenantId)
```

Y en lugar de `ResponseEntity.ok(lista)` usar `ResponseEntity.ok(ApiResponse.ok(lista))`.

El `GlobalExceptionHandler` retorna `ResponseEntity<ErrorResponse>` directamente (sin wrapper `ApiResponse`) porque el gateway inyecta el wrapper si es necesario, o el frontend interpreta el `ErrorResponse` directamente segun el status code. Consistente con auth.

## Tests

Se deben agregar tests unitarios para:
- `GlobalExceptionHandler` — cada handler verifica codigo HTTP + estructura del `ErrorResponse`
- Excepciones custom — constructores y resolucion de `CodigoError`
- `CodigoError` — formateo con `MessageFormat`

No se requiere cobertura completa de integracion para la estrategia — los tests unitarios del handler cubren el comportamiento.
