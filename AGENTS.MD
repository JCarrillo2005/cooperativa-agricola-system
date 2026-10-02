# AGENTS.MD — Sistema de Gestión Integral · Cooperativa Agrícola

> **Documento de contexto técnico y arquitectura** para desarrolladores y agentes de IA.
> Última actualización: 2026-10-01

---

## 0. Alcance

- Proyecto **académico en equipo** (Base de Datos I, Universidad Mariano Gálvez de Guatemala).
- Es una **aplicación de escritorio Java SE** (Swing). **No** es web, ni C#/.NET/WPF, ni Spring.
- Todo el código, comentarios, nombres de tablas y mensajes de interfaz van **en español**.

---

## 1. Descripción General del Proyecto

| Campo                        | Valor                                              |
| ---------------------------- | -------------------------------------------------- |
| **Nombre**                   | Sistema de Gestión Integral - Cooperativa Agrícola |
| **Tipo**                     | Aplicación de escritorio Java SE                   |
| **IDE**                      | Apache NetBeans 31                                 |
| **Lenguaje**                 | Java SE (source/target 1.8)                        |
| **Base de datos principal**  | Oracle Database 10g (XE)                           |
| **Base de datos secundaria** | PostgreSQL (migración y backup en caliente)        |
| **Arquitectura**             | MVC / DAO + capa de controllers                    |
| **Build system**             | Apache Ant                                         |
| **UI**                       | Java Swing (JFrames / GUI Builder)                 |
| **Control de versiones**     | Git + GitHub                                       |

### 1.1 Contexto del negocio

Cooperativa dedicada a dar soluciones a **pequeños agricultores** en el proceso de producción
de productos relacionados con la agricultura. Puede tener **varias sedes** en el país.

Funciones principales:

1. **Créditos (préstamos):** en efectivo, hipotecarios o de **consumo de productos**
   (semillas, abonos, insecticidas, etc.). En los de consumo se registra el detalle de
   los productos entregados.
2. **Cuentas de ahorro:** de ahorro o monetarias. Una cuenta puede pertenecer a
   **varias personas**, y se lleva un registro de saldos.
3. **Recepción de productos de los socios:** los socios entregan hortalizas, verduras, etc.
   mediante **envíos o talonarios de entrega**. Los productos pasan por **control de calidad**
   y, si cumplen, se pagan a un **mejor precio que el de mercado**.
4. **Albarán:** documento que se genera después de la entrega y describe el producto
   **procesado (aceptado)** y el **rechazado**.

### 1.2 Equipo

| Integrante                      | Rol                                                                              |
| ------------------------------- | -------------------------------------------------------------------------------- |
| **Julio César Carrillo García** | Coordinador y desarrollador backend Java (config, model, dao, controller, utils) |
| **José Adrián**                 | Modelado y administración de BD (Oracle / PostgreSQL)                            |
| **Jorge Leonidas**              | Desarrollador frontend Java (Swing / GUI)                                        |
| **Nahomi López**                | Documentadora líder y pruebas                                                    |
| **Marvin Muñoz**                | Documentador y diseñador (resumen ejecutivo / infografía)                        |

**Regla de trabajo:** el backend define el modelo y los contratos (clases `model`, firmas de
los DAO y de los controllers); la BD y la interfaz se adaptan a ellos. Cualquier cambio de
nombres o tipos en `model` debe avisarse al equipo.

### 1.3 Requisitos del curso (enunciado oficial)

El proyecto final se entrega por fases y se evalúa así:

| Parte                                                                                                                | Puntos |
| -------------------------------------------------------------------------------------------------------------------- | ------ |
| Fase I: marco teórico (visión, misión, valores, organigramas, objetivos)                                             | 2      |
| Fase II: análisis y diseño, entidad-relación de la base de datos                                                     | 2      |
| Fase III: implementación de la BD, normalización 1FN a 4FN, cronograma, consejos de implementación y script de la BD | 2      |
| Exposición                                                                                                           | 4      |

**Fase IV (aplicación):**

- Migración de la base de datos a **Firebird u otra base open source** (se usará PostgreSQL).
- **Backup en caliente** con otra base de datos (PostgreSQL).
- **No** se pide generar PDF de albaranes. El PDF no es requisito.

**Entregables del documento:** carátula, carta de acompañamiento, introducción, índice,
cuerpo (fases), conclusiones, recomendaciones, anexos y bibliografía en estilo APA. En los
anexos debe ir **una buena práctica implementada con evidencia** (propuesta del backend:
transacciones con rollback, ver sección 16).

**Entrega y exposición:** documento impreso y enviado por correo un día antes, antes de las
23:59. El software completo se entrega en CD/DVD. Exposición de 25 minutos (unos 5 por
integrante), con al menos 5 conclusiones, una infografía y un resumen ejecutivo de 3 a 5
páginas. Las fechas de cada fase están en blanco en el enunciado: confirmarlas con el catedrático.

---

## 2. Estructura del Repositorio

```
SistemaCooperativaAgricola/
├── src/
│   └── com/cooperativa/
│       ├── config/
│       │   ├── ConexionOracle.java      # Pool de conexiones a Oracle
│       │   └── ConexionPostgres.java    # (pendiente, Fase IV)
│       ├── model/                       # Entidades POJO (15 clases)
│       ├── dao/                         # Data Access Objects (15 clases)
│       ├── controller/                  # Reglas de negocio (5 controllers + 2 utilitarias)
│       ├── utils/
│       │   └── BackupEnCaliente.java    # (pendiente, Fase IV)
│       ├── view/                        # (pendiente) Interfaces Swing
│       └── main/
│           └── Main.java                # Hoy: arnés de pruebas manual
├── lib/
│   ├── ojdbc14.jar
│   └── postgresql-42.7.13.jar
├── scripts/                             # (pendiente) DDL/DML Oracle y PostgreSQL
├── nbproject/                           # Configuración NetBeans (private/ se ignora)
├── build.xml
├── manifest.mf
├── config.properties.example            # Plantilla de credenciales (SÍ se sube a Git)
├── config.properties                    # Credenciales reales (NO se sube a Git)
├── README.md
└── AGENTS.MD
```

> **Estado real de las carpetas:** existen `config/`, `model/`, `dao/`, `controller/` y `main/`.
> `view/`, `utils/` y `scripts/` **todavía no están creadas**.

---

## 3. Arquitectura del Sistema

### 3.1 Flujo MVC / DAO

```
VIEW (Swing)  →  CONTROLLER (lógica de negocio)  →  DAO (SQL / JDBC)  →  Oracle 10g
                                   ↑
                           MODEL (POJOs) viaja entre todas las capas

UTILS (BackupEnCaliente)  →  lee Oracle por JDBC  →  escribe en PostgreSQL   (Fase IV)
```

- La **vista** nunca habla con un DAO ni escribe SQL: solo llama a los controllers.
- El **controller** valida reglas (saldo suficiente, estatus activo, stock) y decide qué DAOs
  se llaman y dentro de qué transacción. Informa los errores con `NegocioException`.
- El **DAO** solo traduce entre objetos `model` y SQL. No abre transacciones por su cuenta.

### 3.2 Estado por capa

| Capa           | Paquete                      | Responsabilidad                   | Estado                              |
| -------------- | ---------------------------- | --------------------------------- | ----------------------------------- |
| **Config**     | `com.cooperativa.config`     | Pool de conexiones JDBC           | ✅ Oracle · ❌ PostgreSQL pendiente |
| **Model**      | `com.cooperativa.model`      | 15 entidades POJO                 | ✅ Implementada                     |
| **DAO**        | `com.cooperativa.dao`        | CRUD y operaciones por entidad    | ✅ Implementada (15 DAOs)           |
| **Controller** | `com.cooperativa.controller` | Reglas de negocio y transacciones | ✅ Implementada y probada           |
| **View**       | `com.cooperativa.view`       | Pantallas Swing                   | ❌ No existe aún                    |
| **Utils**      | `com.cooperativa.utils`      | Backup en caliente a PostgreSQL   | ❌ No existe aún                    |
| **Main**       | `com.cooperativa.main`       | Arnés de pruebas manual           | ✅ (temporal)                       |

### 3.3 Por qué se implementaron `model` y `dao` primero

Es una decisión de la regla de trabajo del equipo (sección 1.2), no un descuido:

1. **`model` fija el contrato.** Las 15 clases POJO definen los nombres de campo y tipos
   que la BD y la interfaz van a consumir. Si el modelo cambia después, se rompen el DDL,
   los `mapear()` de los DAO y las pantallas.
2. **`dao` cierra el circuito con la BD.** Los DAO fijan los **nombres de tabla y columna**
   (`personas.tipo_persona_id`, `detalles_albaran.cantidad_aceptada`, …). José necesita
   esas columnas exactas para escribir el DDL, así que los DAO son la especificación que
   consume el modelado de BD.
3. **`controller`, `view` y `utils` dependen de ambos.** El controller llama a firmas de
   DAO ya decididas; la vista consume POJOs ya cerrados.
4. **Se validó por capas.** Cada DAO y cada controller se probó contra Oracle con `Main`
   antes de avanzar a la capa siguiente.

---

## 4. Configuración del Proyecto

### 4.1 `nbproject/project.properties`

```properties
javac.source=1.8
javac.target=1.8
source.encoding=UTF-8
main.class=com.cooperativa.main.Main

file.reference.ojdbc14.jar=lib/ojdbc14.jar
file.reference.postgresql-42.7.13.jar=lib/postgresql-42.7.13.jar

build.dir=build
dist.dir=dist
dist.jar=${dist.dir}/SistemaCooperativaAgricola.jar
src.dir=src
test.src.dir=test
```

> ⚠️ **Verificar el archivo real.** En la última revisión (2026-10-01) el
> `nbproject/project.properties` contenía valores que **no** coinciden con el bloque de arriba:
>
> ```properties
> javac.source=26
> javac.target=26
> file.reference.ojdbc14.jar=C:\\oraclexe\\app\\oracle\\product\\10.2.0\\server\\jdbc\\lib\\ojdbc14.jar
> file.reference.postgresql-42.7.13.jar=C:\\Users\\Julio Cesar\\Downloads\\postgresql-42.7.13.jar
> ```
>
> Los valores del bloque son la **configuración objetivo**. Con las rutas absolutas el
> proyecto compila en la máquina de Julio, pero **rompe al clonar el repo** en cualquier
> otra. Corregir antes de volver a hacer commit.

### 4.2 Reglas importantes

**Versión de Java.** `ojdbc14.jar` es un driver legado (Java 1.4/5). No subir
`javac.source` / `javac.target` por encima de 1.8 sin migrar antes a un driver
moderno (`ojdbc8` / `ojdbc11`).

| Versión de Java | ¿Compatible con `ojdbc14.jar`? |
| --------------- | ------------------------------ |
| **Java 8**      | ✅ Recomendada                 |
| **Java 11**     | ✅ Alternativa válida          |
| Java 17+        | ⚠️ Riesgo de incompatibilidad  |

**Sintaxis permitida:** código compatible con Java 8. **No usar** `var`, `record`,
text blocks (`"""`), `List.of()` ni la API `java.time` con JDBC (el driver no la soporta).

**Rutas relativas.** Los JAR se referencian desde `lib/`. Nunca rutas absolutas
(`C:\Users\...`), porque rompen la compilación al clonar el repo. Si un JAR cambia de
nombre o versión, actualizar el archivo en `lib/` **y** las líneas `file.reference.*`.

### 4.3 Credenciales de BD (`config.properties`)

Las credenciales **no** están en el código. Cada integrante crea en la raíz del proyecto su
propio `config.properties` a partir de `config.properties.example`:

```properties
db.url=jdbc:oracle:thin:@localhost:1521:XE
db.user=TU_USUARIO
db.password=TU_CONTRASENA
```

`config.properties` está en `.gitignore`. **Nunca** commitear credenciales.

| Parámetro (Oracle) | Valor                                 |
| ------------------ | ------------------------------------- |
| URL                | `jdbc:oracle:thin:@localhost:1521:XE` |
| Driver             | `oracle.jdbc.driver.OracleDriver`     |

Para la Fase IV se agregarán al mismo archivo las claves `pg.url`, `pg.user` y `pg.password`
(PostgreSQL: driver `org.postgresql.Driver`, JAR `postgresql-42.7.13.jar`, puerto `5432`).

**Todo el equipo debe usar el mismo usuario/esquema de Oracle**, para que las tablas y
secuencias existan igual en todas las máquinas. En desarrollo hoy se usa `SYSTEM`; para la
entrega conviene un esquema propio (por ejemplo `COOPERATIVA`).

---

## 5. Dependencias

| JAR                      | Ubicación | Descripción            |
| ------------------------ | --------- | ---------------------- |
| `ojdbc14.jar`            | `lib/`    | Driver JDBC Oracle 10g |
| `postgresql-42.7.13.jar` | `lib/`    | Driver JDBC PostgreSQL |

| Tecnología      | Versión          | Uso                                  |
| --------------- | ---------------- | ------------------------------------ |
| Java SE         | 1.8              | Lenguaje principal                   |
| Apache NetBeans | 31               | IDE                                  |
| Oracle Database | 10g (10.2.0.1.0) | BD principal                         |
| PostgreSQL      | -                | BD secundaria (migración y respaldo) |
| Apache Ant      | -                | Build                                |
| Java Swing      | -                | UI                                   |

---

## 6. Capa Config: `ConexionOracle`

Fábrica de conexiones con un **pool sencillo** (sin librerías externas).

```java
public static Connection getConexion() throws SQLException   // entrega una conexión del pool
public static boolean probarConexion()                       // true si conecta
public static void cerrarTodas()                             // cierra las conexiones libres (al salir de la app)
```

Cómo funciona:

- Lee `config.properties` y carga el driver en un bloque `static`. Si falta el archivo o el
  driver, lanza `ExceptionInInitializerError` con un mensaje claro.
- Mantiene como máximo **10 conexiones físicas** abiertas y espera hasta **15 segundos** si
  todas están en uso.
- Entrega la conexión envuelta en un `Proxy`: al hacer `close()` **no se cierra de verdad**,
  vuelve al pool. Si estaba con `autoCommit = false`, el pool hace `rollback` y restaura
  `autoCommit = true` antes de reutilizarla.
- El uso no cambia: sigue siendo try-with-resources.

```java
try (Connection conn = ConexionOracle.getConexion()) {
    // usar conn
} catch (SQLException e) {
    // manejar error
}
```

**Por qué hay pool:** abrir y cerrar una conexión física por cada consulta saturaba el
listener de Oracle XE (`ORA-12519`) cuando un controller encadenaba varias operaciones.

**Importante:** Oracle **confirma (`commit`) lo pendiente al cerrar una conexión física**.
Por eso toda transacción debe hacer `commit` o `rollback` explícito (sección 9.6).

---

## 7. Capa Model: entidades del dominio

Paquete `com.cooperativa.model`. POJOs con constructor vacío, constructor útil, getters/setters
y `toString()` en los catálogos.

### 7.1 Clases

| Clase               | Descripción                                                         |
| ------------------- | ------------------------------------------------------------------- |
| `Sede`              | Sedes de la cooperativa en el país                                  |
| `Estatus`           | Catálogo general de estatus (activo, inactivo, pagado, vencido…)    |
| `TipoPersona`       | Empleado, socio u otro                                              |
| `Persona`           | Empleados, socios y otras personas; tiene tipo, estatus y sede      |
| `TipoCuenta`        | Ahorro, monetaria                                                   |
| `Cuenta`            | Cuenta con tipo, estatus y sede                                     |
| `CuentaPersona`     | Tabla puente: una cuenta puede tener varias personas (`titular`)    |
| `Saldo`             | Registro de saldos de una cuenta por fecha                          |
| `TipoPrestamo`      | Efectivo, hipotecario, consumo de productos                         |
| `Prestamo`          | Registro del préstamo (monto, tasa, fechas, tipo, estatus)          |
| `DetallePrestamo`   | Productos entregados en un préstamo de consumo                      |
| `CategoriaProducto` | Insumos para socios vs. productos de exportación                    |
| `Producto`          | Catálogo de productos (insumos y productos que entregan los socios) |
| `Albaran`           | Documento de una entrega por talonario                              |
| `DetalleAlbaran`    | Línea del albarán: cantidad aceptada y rechazada, precio            |

### 7.2 Relaciones

```
Sede (1) ───< Persona (N)            Sede (1) ───< Cuenta (N)         Sede (1) ───< Albaran (N)
TipoPersona (1) ───< Persona (N)     TipoCuenta (1) ───< Cuenta (N)   TipoPrestamo (1) ───< Prestamo (N)
Estatus (1) ───< Persona / Cuenta / Prestamo (N)

Cuenta (N) >───< Persona (N)         [tabla puente CuentaPersona]
Cuenta (1) ───< Saldo (N)
Persona (1) ───< Prestamo (N)
Prestamo (1) ───< DetallePrestamo (N) >─── (1) Producto
CategoriaProducto (1) ───< Producto (N)
Persona (1) ───< Albaran (N)
Albaran (1) ───< DetalleAlbaran (N) >─── (1) Producto
```

### 7.3 Convenciones de mapeo POJO ↔ Oracle

| Java                        | Oracle 10g                   | Notas                                                                |
| --------------------------- | ---------------------------- | -------------------------------------------------------------------- |
| `int id`                    | `NUMBER(10)` PK              | Secuencia `seq_<tabla>`; el DAO pide `NEXTVAL`. **No hay triggers.** |
| `String`                    | `VARCHAR2(n)`                |                                                                      |
| `BigDecimal` (dinero)       | `NUMBER(12,2)`               | Nunca `double` para dinero                                           |
| `BigDecimal` (tasa)         | `NUMBER(5,2)`                |                                                                      |
| `BigDecimal` (cantidad, kg) | `NUMBER(12,2)`               | En `DetalleAlbaran`                                                  |
| `int` (stock, cantidad)     | `NUMBER(10)`                 | `Producto.stock` y `DetallePrestamo.cantidad`                        |
| `java.util.Date`            | `DATE`                       | En el DAO: `new java.sql.Date(f.getTime())`                          |
| `boolean`                   | `NUMBER(1)` (0/1)            | Oracle 10g no tiene `BOOLEAN` en SQL                                 |
| `campoId` (FK)              | `NUMBER(10)` + `FOREIGN KEY` | `personaId` → columna `persona_id`                                   |

- Los nombres de columna son el campo en `snake_case` (`fechaApertura` → `fecha_apertura`).
- Tablas en plural y `snake_case`: `personas`, `cuentas`, `cuentas_personas`, `saldos`,
  `prestamos`, `detalles_prestamo`, `productos`, `albaranes`, `detalles_albaran`,
  `sedes`, `estatus`, `tipos_persona`, `tipos_cuenta`, `tipos_prestamo`, `categorias_producto`.
- Oracle trata la cadena vacía `''` como `NULL`; tenerlo en cuenta en validaciones.
- Paginación con `ROWNUM` (10g no tiene `LIMIT` ni `FETCH FIRST`).

### 7.4 Lógica de negocio dentro de `model` (excepción consciente)

Cuatro métodos llevan comportamiento, no solo datos. Es intencional: son cálculos que
dependen **solo** del propio objeto, sin BD ni reglas de negocio.

| Método                | Clase                 | Qué hace                               |
| --------------------- | --------------------- | -------------------------------------- |
| `calcularTotal()`     | `Albaran`             | Suma los subtotales de los detalles    |
| `getSubtotal()`       | `DetalleAlbaran`      | `cantidadAceptada × precioUnitario`    |
| `getSubtotal()`       | `DetallePrestamo`     | `precioUnitario × cantidad`            |
| `agregarDetalle(...)` | `Albaran`, `Prestamo` | Añade una línea a la lista de detalles |

`Prestamo` y `Albaran` son los únicos con lista de detalles, inicializada a
`new ArrayList<...>()` para que nunca sea `null`.

**El total del albarán se calcula solo con lo aceptado**, nunca con la cantidad entregada:
lo rechazado no se le paga al socio.

---

## 8. Capa DAO: los 15 DAOs implementados

`com.cooperativa.dao` tiene **un DAO por entidad de `model`**. Los 15 compilan y están
probados contra Oracle.

| DAO                    | Tabla                 | Métodos propios además del CRUD estándar                                                                                                  |
| ---------------------- | --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `SedeDAO`              | `sedes`               | —                                                                                                                                         |
| `EstatusDAO`           | `estatus`             | `obtenerPorNombre(String)` (ignora mayúsculas)                                                                                            |
| `TipoPersonaDAO`       | `tipos_persona`       | —                                                                                                                                         |
| `TipoCuentaDAO`        | `tipos_cuenta`        | —                                                                                                                                         |
| `TipoPrestamoDAO`      | `tipos_prestamo`      | —                                                                                                                                         |
| `CategoriaProductoDAO` | `categorias_producto` | —                                                                                                                                         |
| `PersonaDAO`           | `personas`            | `obtenerPorDni()`, `obtenerPorTipo()`, `cambiarEstatus()`                                                                                 |
| `CuentaDAO`            | `cuentas`             | `obtenerPorNumero()`, `obtenerPorPersona()` (JOIN con `cuentas_personas`), `cambiarEstatus()`, **`bloquear()`** (`SELECT ... FOR UPDATE`) |
| `CuentaPersonaDAO`     | `cuentas_personas`    | `agregar()`, `cambiarTitular()`, `quitar()`, `obtenerPorCuenta()`, `obtenerPorPersona()`                                                  |
| `SaldoDAO`             | `saldos`              | `registrar()`, `obtenerActual()` (con `ROWNUM = 1`), `obtenerHistorial()`                                                                 |
| `PrestamoDAO`          | `prestamos`           | `cambiarEstatus()`, `obtenerPorIdConDetalles()`, `obtenerPorPersona()`                                                                    |
| `DetallePrestamoDAO`   | `detalles_prestamo`   | `guardar()`, `obtenerPorPrestamo()`                                                                                                       |
| `ProductoDAO`          | `productos`           | `obtenerPorCategoria()`, **`descontarStock()`**, **`aumentarStock()`**                                                                    |
| `AlbaranDAO`           | `albaranes`           | `obtenerPorNumeroTalonario()`, `obtenerPorIdConDetalles()`, `obtenerPorPersona()`                                                         |
| `DetalleAlbaranDAO`    | `detalles_albaran`    | `guardar()`, `obtenerPorAlbaran()`                                                                                                        |

### 8.1 Patrones que siguen TODOS los DAO

**a) Constante `COLUMNAS` y SQL sin `SELECT *`.**
Los DAO principales (`Persona`, `Cuenta`, `Prestamo`, `Producto`, `Albaran`) declaran
`private static final String COLUMNAS` y la concatenan en cada consulta. Los DAO de
catálogo y de línea de detalle escriben la lista de columnas en el literal.

**b) Sobrecarga con `Connection` para transacciones.**
Es el patrón central del proyecto. La versión sin `Connection` abre y cierra la suya con
try-with-resources (commit automático); la versión con `Connection` **no abre ni cierra**
nada, para poder encadenarla dentro de la transacción de un controller:

```java
public boolean guardar(Persona p) throws SQLException {
    try (Connection conn = ConexionOracle.getConexion()) {
        return guardar(p, conn);   // delega
    }
}

public boolean guardar(Persona p, Connection conn) throws SQLException { ... }
```

**c) ID con secuencia Oracle, nunca con trigger ni `IDENTITY`.**
Cada DAO tiene un `siguienteId(Connection)` privado que pide el valor a la secuencia
(Oracle 10g con `ojdbc14` no soporta `getGeneratedKeys`):

```java
private int siguienteId(Connection conn) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement("SELECT seq_personas.NEXTVAL FROM dual");
         ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
    }
}
```

Secuencias: `seq_personas`, `seq_cuentas`, `seq_saldos`, `seq_prestamos`,
`seq_detalles_prestamo`, `seq_productos`, `seq_albaranes`, `seq_detalles_albaran`,
`seq_sedes`, `seq_estatus`, `seq_tipos_persona`, `seq_tipos_cuenta`, `seq_tipos_prestamo`,
`seq_categorias_producto`. La tabla `cuentas_personas` no tiene id propio: su llave
primaria es `(cuenta_id, persona_id)`.

**d) Método privado `mapear(ResultSet)`.** Cada DAO tiene un único `mapear` que construye el POJO.

**e) Fechas de alta con `SYSDATE`, no desde Java.**
`personas.fecha_registro`, `albaranes.fecha`, `prestamos.fecha`, `saldos.fecha` y
`cuentas.fecha_apertura` se llenan en el `INSERT` con `SYSDATE`.

**f) `null` cuando no hay resultado.** Todos los `obtenerPorId` devuelven `null` si el
`ResultSet` está vacío (no lanzan excepción).

**g) Fechas de lectura con `getTimestamp`** (conserva la hora). Al escribir, en
`PrestamoDAO` se usa `new java.sql.Date(f.getTime())` y `setNull(..., Types.DATE)` si es `null`.

**h) Booleanos como `NUMBER(1)`.** `CuentaPersonaDAO` mapea `titular` con
`rs.getInt("titular") == 1` y escribe `cp.isTitular() ? 1 : 0`.

### 8.2 Notas de diseño por DAO

**`ProductoDAO.descontarStock(int, int, Connection)`** descuenta stock de forma atómica con
una sola sentencia y devuelve `false` si no hay inventario, sin modificar nada:

```java
String sql = "UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?";
```

Solo existe con `Connection`: siempre se usa dentro de la transacción de un préstamo de
consumo. `aumentarStock` suma sin condición.

**`SaldoDAO.obtenerActual`** devuelve el saldo vigente, que es el registro más reciente.
`ROWNUM` se evalúa **antes** del `ORDER BY`, por eso el orden va en una subconsulta:

```sql
SELECT id, cuenta_id, monto, fecha FROM (
  SELECT id, cuenta_id, monto, fecha FROM saldos
  WHERE cuenta_id = ? ORDER BY fecha DESC, id DESC
) WHERE ROWNUM = 1
```

Existe con y sin `Connection` porque hay que **leer el saldo antes de modificarlo** dentro de
la misma transacción. La tabla `saldos` es un **historial**: solo se agregan filas, no se
actualizan ni se borran.

**`CuentaDAO.bloquear(int, Connection)`** bloquea la fila de la cuenta (`FOR UPDATE`) hasta el
fin de la transacción, para que dos depósitos o retiros simultáneos no lean el mismo saldo.

**`CuentaDAO.obtenerPorPersona(int)`** resuelve el muchos-a-muchos de `cuentas_personas` con un
`JOIN`. Trae cualquier persona asociada, no solo titulares.

**Borrados físicos con `ORA-02292`.** `eliminar()` hace `DELETE` directo. Si la fila ya está
referenciada, Oracle lanza `ORA-02292` y **es intencional**. Para dar de baja sin borrar está
`cambiarEstatus(id, estatusId)` en `PersonaDAO`, `CuentaDAO` y `PrestamoDAO`.

### 8.3 Encabezado + detalle (composición)

`AlbaranDAO.obtenerPorIdConDetalles(int)` y `PrestamoDAO.obtenerPorIdConDetalles(int)` llaman a
su `obtenerPorId` y luego inyectan la lista con el DAO de detalle. El DAO de detalle no sabe
nada del encabezado: solo usa `getAlbaranId()` / `getPrestamoId()`, que **debe venir ya
asignado por quien guarda**, después de guardar el encabezado.

`DetalleAlbaranDAO` y `DetallePrestamoDAO` **no implementan `actualizar()` ni `eliminar()`**:
son líneas inmutables de su encabezado. Si una línea cambia, se rehace el albarán o el préstamo.
Tampoco `AlbaranDAO` ni `PrestamoDAO` tienen `eliminar()`: son documentos; un préstamo se cierra
cambiando su estatus.

### 8.4 Qué NO hay todavía en la capa DAO

- **Sin transacciones dentro de los DAO.** Por diseño: las abre y cierra el controller.
- **Sin consultas con nombres resueltos.** Los DAO devuelven ids (`sedeId`); la vista necesitará
  `JOIN` para mostrar "Sede Central" en una tabla.
- **Sin paginación** ni uso de `ROWNUM` en listados (solo en `obtenerActual`).
- **Sin búsqueda de texto** (`LIKE` por nombre, DNI parcial, talonario parcial).
- **Sin operaciones en lote.**
- **Sin soporte PostgreSQL.** Todo el SQL de los DAO es Oracle 10g.

---

## 9. Capa Controller

Paquete `com.cooperativa.controller`. Es la capa que usará la vista. Cada método:
valida, aplica las reglas de negocio, ejecuta la transacción necesaria y, si algo falla,
lanza `NegocioException` con un mensaje listo para mostrar al usuario.

### 9.1 Clases

| Clase                | Qué hace                                                                                                                                                         |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `NegocioException`   | Excepción verificada con mensaje para el usuario. `deSQL(contexto, e)` traduce errores de Oracle                                                                 |
| `Catalogos`          | Obtiene ids de catálogos por nombre (`estatusId`, `tipoPersonaId`, `tipoPrestamoId`)                                                                             |
| `PersonaController`  | `registrar`, `actualizar`, `activar`, `desactivar`, `cambiarEstatus`, `obtenerPorId`, `buscarPorDni`, `obtenerActiva`, `listarTodas`, `listarSocios`, `eliminar` |
| `CuentaController`   | `abrirCuenta`, `depositar`, `retirar`, `consultarSaldo`, `historialSaldos`, `cuentasDePersona`, `personasDeCuenta`, `agregarPersona`, `quitarPersona`            |
| `PrestamoController` | `registrarPrestamo`, `cambiarEstatus`, `obtener`, `listarTodos`, `listarPorPersona`                                                                              |
| `AlbaranController`  | `registrarEntrega`, `totalAPagar`, `obtener`, `buscarPorTalonario`, `listarTodos`, `listarPorSocio`                                                              |
| `ProductoController` | `registrar`, `actualizar`, `cambiarPrecio`, `agregarStock`, `obtenerPorId`, `listarTodos`, `listarPorCategoria`, `listarStockBajo`, `eliminar`                   |

Convenciones de retorno: los `obtener...` por id **lanzan** `NegocioException` si no existe
(salvo `buscarPorDni` y `buscarPorTalonario`, que devuelven `null`); los `listar...` devuelven
listas (vacías si no hay datos).

### 9.2 `NegocioException.deSQL`

Traduce códigos de Oracle a mensajes entendibles:

| Código Oracle | Mensaje                                                                          |
| ------------- | -------------------------------------------------------------------------------- |
| `ORA-00001`   | Ya existe un registro con ese valor único (DNI, número de cuenta, talonario)     |
| `ORA-02291`   | Hace referencia a un dato que no existe (sede, tipo, estatus, persona, producto) |
| `ORA-02292`   | Tiene registros asociados y no se puede eliminar                                 |
| `ORA-12899`   | Un valor es demasiado largo para su campo                                        |
| otros         | Error de base de datos con el mensaje original                                   |

### 9.3 `Catalogos`: nombres que deben existir en la BD

Los controllers localizan estos registros **por nombre, sin distinguir mayúsculas**
(`Activo` y `ACTIVO` valen igual):

| Constante               | Nombre              | Tabla            |
| ----------------------- | ------------------- | ---------------- |
| `ESTATUS_ACTIVO`        | `ACTIVO`            | `estatus`        |
| `ESTATUS_INACTIVO`      | `INACTIVO`          | `estatus`        |
| `TIPO_PERSONA_SOCIO`    | `SOCIO`             | `tipos_persona`  |
| `TIPO_PRESTAMO_CONSUMO` | `CONSUMO_PRODUCTOS` | `tipos_prestamo` |

Si alguno falta en el catálogo, el controller lanza `NegocioException`. Cualquier script de
datos iniciales debe incluirlos. Los nombres de las categorías de producto **no** se usan en
el código.

### 9.4 Reglas de negocio implementadas

**Personas**

- Nombre (≤60), apellido (≤60) y DNI (≤20) obligatorios; dirección ≤150 y teléfono ≤20.
- DNI único. Tipo, estatus y sede deben existir. Si no trae estatus, queda `ACTIVO`.
- `obtenerActiva(id)` es la regla "solo una persona activa puede operar" y la reutilizan los demás controllers.
- `eliminar` falla con mensaje claro si tiene cuentas, préstamos o entregas; se recomienda desactivar.

**Cuentas**

- Al abrir: al menos una persona, **al menos un titular**, todas activas y sin repetir; tipo y sede existentes;
  saldo inicial ≥ 0; número único (si no viene, se genera `CTA-` + sufijo de la hora).
  Cuenta + personas + saldo inicial se guardan en **una transacción**.
- Depósitos y retiros: monto > 0 con 2 decimales, cuenta activa, bloqueo `FOR UPDATE`, y el saldo
  **nunca queda negativo**. Cada movimiento agrega una fila nueva a `saldos`.
- No se puede quitar a la última persona titular; una persona no se repite en la misma cuenta.

**Préstamos**

- Solo personas activas. Tasa ≥ 0. Fecha de vencimiento, si viene, debe ser futura. Estatus inicial `ACTIVO`.
- **Consumo de productos:** al menos un producto, cantidades > 0, el **precio sale del catálogo**
  (no del que mande la vista), monto = suma de subtotales, y se **descuenta el stock**
  (si no alcanza: `Stock insuficiente de '<producto>' (disponible: N)` y no se guarda nada).
- **Efectivo / hipotecario:** no lleva productos y el monto debe ser > 0.

**Albaranes**

- Talonario obligatorio (≤30) y **único**. Solo **socios activos** pueden entregar.
- Si no trae sede, se usa la del socio. Al menos una línea; cada producto debe existir;
  cantidades ≥ 0 y no ambas en cero; si no trae precio, se usa el del catálogo.
- El total a pagar cuenta **solo lo aceptado** (`totalAPagar`).
- **El albarán no modifica `Producto.stock`** (ver sección 15).

**Productos**

- Nombre (≤80) obligatorio, descripción ≤200, categoría existente, precio > 0 (2 decimales), stock ≥ 0.
- `agregarStock` exige cantidad > 0. `cambiarPrecio` no altera préstamos ya registrados,
  porque cada `DetallePrestamo` guarda su propio `precio_unitario`.

### 9.5 Cómo usa la vista un controller

```java
try {
    new PersonaController().registrar(persona);
    JOptionPane.showMessageDialog(this, "Persona registrada.");
} catch (NegocioException ex) {
    JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
}
```

### 9.6 Patrón de transacción en los controllers

```java
try (Connection conn = ConexionOracle.getConexion()) {
    conn.setAutoCommit(false);
    try {
        prestamoDAO.guardar(p, conn);
        for (DetallePrestamo d : p.getDetalles()) {
            if (!productoDAO.descontarStock(d.getProductoId(), d.getCantidad(), conn)) {
                throw new NegocioException("Stock insuficiente ...");
            }
            d.setPrestamoId(p.getId());               // el DAO de detalle no lo adivina
            detallePrestamoDAO.guardar(d, conn);
        }
        conn.commit();
    } catch (SQLException | RuntimeException | NegocioException e) {
        conn.rollback();                              // incluye errores de negocio
        throw e;
    }
}
```

Se hace `rollback` ante **cualquier** excepción, no solo `SQLException`, porque Oracle confirma
lo pendiente al cerrar la conexión (sección 6).

### 9.7 Qué falta en la capa Controller

- **Controller de catálogos** para administrar sedes, tipos, estatus y categorías (hoy solo existen sus DAOs).
- **Consultas con nombres resueltos**, búsquedas por texto y paginación, cuando Jorge defina las pantallas.
- **Pago al socio por un albarán** y **abonos a préstamos** (ver dudas abiertas, sección 15).

---

## 10. Guías de Desarrollo

### 10.1 Nomenclatura

| Elemento   | Convención       | Ejemplo                           |
| ---------- | ---------------- | --------------------------------- |
| Clases     | PascalCase       | `PersonaDAO`, `ConexionOracle`    |
| Métodos    | camelCase        | `obtenerPorId()`, `guardar()`     |
| Variables  | camelCase        | `nombreCliente`, `listaProductos` |
| Constantes | UPPER_SNAKE_CASE | `ARCHIVO_CONFIG`, `COLUMNAS`      |
| Paquetes   | minúsculas       | `com.cooperativa.dao`             |

Los nombres de métodos, comentarios y mensajes de error van en español:
`obtenerPorId`, `obtenerTodos`, `cambiarEstatus`, `descontarStock`, `registrar`, `agregarDetalle`.

### 10.2 Cómo escribir un DAO nuevo (checklist)

1. Un DAO por entidad de `model`, mismo nombre con sufijo `DAO`.
2. `private static final String COLUMNAS` con la lista explícita de columnas.
3. `siguienteId(Connection)` con `SELECT seq_<tabla>.NEXTVAL FROM dual`.
4. Par de métodos `guardar(X)` y `guardar(X, Connection)`, con delegación entre ambos.
5. `obtenerPorId`, `obtenerTodos`, `actualizar`, `eliminar` con `throws SQLException`.
6. `private X mapear(ResultSet)` como único constructor de POJO desde BD.
7. Todo `SELECT` con columnas explícitas y `ORDER BY` estable.
8. Fechas de alta con `SYSDATE` en el SQL; fechas de lectura con `getTimestamp`.
9. Recursos con try-with-resources, incluido el `ResultSet`.
10. Javadoc en español en los métodos con comportamiento no obvio (transaccional, borrado que
    falla por FK, descuento condicional de stock).

### 10.3 Patrón de consulta (ejemplo real, `SedeDAO`)

```java
public Sede obtenerPorId(int id) throws SQLException {
    String sql = "SELECT id, nombre, direccion, departamento FROM sedes WHERE id = ?";
    try (Connection conn = ConexionOracle.getConexion();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, id);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapear(rs);
            }
        }
    }
    return null;
}
```

- **Siempre** `PreparedStatement` (evita SQL Injection). Nunca concatenar valores en el SQL.
- **Nunca** `SELECT *`: el orden de columnas no está garantizado y el mapeo se rompe.

### 10.4 UI (Swing)

- Iniciar la UI con `SwingUtilities.invokeLater()`.
- Usar `SwingWorker` para consultas a BD, para no congelar la ventana.
- La vista solo llama a los controllers y muestra `NegocioException.getMessage()`; no contiene SQL ni reglas de negocio.
- Al cerrar la aplicación, llamar a `ConexionOracle.cerrarTodas()`.

### 10.5 Seguridad

- Credenciales solo en `config.properties` (ignorado por Git).
- Solo `PreparedStatement`.
- No imprimir contraseñas ni la cadena de conexión completa en logs.

---

## 11. Fase IV: migración y backup en caliente a PostgreSQL

> **Estado: diseño propuesto, no implementado.** PostgreSQL aún no está instalado en la
> máquina de Julio.

El enunciado pide migrar a una base open source y mantener un backup en caliente con
PostgreSQL. Un solo módulo cubre ambas cosas: la **migración** es una copia completa de una vez,
y el **backup en caliente** es la misma copia repetida mientras la aplicación sigue en uso.

**Piezas planeadas**

- `config/ConexionPostgres.java`: fábrica de conexiones a PostgreSQL (claves `pg.url`,
  `pg.user`, `pg.password` en `config.properties`; base `cooperativa_respaldo`).
- `utils/BackupEnCaliente.java`: copia todas las tablas de Oracle a PostgreSQL.
- `scripts/`: DDL consolidado para Oracle y su equivalente para PostgreSQL.

**Cómo debe funcionar el respaldo**

1. Abre una conexión a Oracle y ejecuta `SET TRANSACTION READ ONLY`: da una **foto consistente**
   de todas las tablas aunque otros usuarios sigan escribiendo (eso es lo "en caliente").
2. Copia tabla por tabla, en lotes (`addBatch` / `executeBatch`), a PostgreSQL dentro de **una transacción**.
3. Orden de copia (respeta las llaves foráneas): `sedes`, `estatus`, `tipos_persona`,
   `tipos_cuenta`, `tipos_prestamo`, `categorias_producto`, `personas`, `productos`, `cuentas`,
   `cuentas_personas`, `saldos`, `prestamos`, `detalles_prestamo`, `albaranes`, `detalles_albaran`.
4. En cada respaldo vacía las tablas destino y las vuelve a llenar (versión incremental = mejora futura).
5. **Verifica** comparando el conteo de filas de cada tabla entre Oracle y PostgreSQL.

**Equivalencia de tipos**

| Oracle         | PostgreSQL      |
| -------------- | --------------- |
| `NUMBER(10)`   | `INTEGER`       |
| `NUMBER(12,2)` | `NUMERIC(12,2)` |
| `NUMBER(5,2)`  | `NUMERIC(5,2)`  |
| `NUMBER(1)`    | `SMALLINT`      |
| `VARCHAR2(n)`  | `VARCHAR(n)`    |
| `DATE`         | `TIMESTAMP`     |

El módulo de respaldo **no usa los DAO** (son Oracle-específicos: `NEXTVAL`, `SYSDATE`, `ROWNUM`);
copia por JDBC directamente.

---

## 12. Comandos de Build y Ejecución

```bash
ant clean
ant compile      # compilar
ant jar          # generar el JAR en dist/
```

Desde NetBeans: **F11** (Clean and Build) y **F6** (Run).
Por línea de comandos, el separador del classpath es `;` en Windows y `:` en Linux/Mac:

```bash
java -cp "build/classes;lib/ojdbc14.jar;lib/postgresql-42.7.13.jar" com.cooperativa.main.Main
```

`config.properties` debe estar en el directorio desde donde se ejecuta (la raíz del proyecto).

`.gitignore`:

```
build/
dist/
nbproject/private/
*.class
config.properties
```

---

## 13. Flujo de trabajo con Git

- `main` siempre debe compilar. No se trabaja directamente sobre `main`.
- **Una rama por tarea**, con prefijo `feature/`: `feature/model`, `feature/dao-catalogos`,
  `feature/controllers`, `feature/scripts-sql`, `feature/vista-personas`.
- Mensajes de commit cortos y en español: `Agrega clases del paquete model`.
- Integración mediante **Pull Request** revisado por otro integrante.
- Antes de seguir trabajando, traer lo nuevo de `main` a la rama propia.
- Antes de commitear, revisar `git diff`: NetBeans puede modificar `nbproject/project.properties`
  con rutas absolutas; no subir ese cambio. No subir `config.properties`.

```bash
git checkout main && git pull origin main
git checkout -b feature/nombre-de-la-tarea
git add . && git commit -m "Descripción del cambio"
git push -u origin feature/nombre-de-la-tarea
```

El estado de ramas y commits cambia a diario: consultarlo con `git status`, `git branch` y `git log`.

---

## 14. Estado del Proyecto

### 14.1 Completado

- [x] Estructura de carpetas y configuración NetBeans
- [x] `ConexionOracle` con `config.properties` y **pool de conexiones**
- [x] Capa Model: 15 clases POJO
- [x] Capa DAO: 15 clases, probadas contra Oracle
- [x] Capa Controller: `NegocioException`, `Catalogos` y 5 controllers, probados contra Oracle
- [x] Transacciones con rollback: apertura de cuenta, retiros y depósitos con bloqueo,
      préstamo de consumo con descuento de stock, albarán con sus líneas
- [x] Reglas de negocio de la sección 9.4 (persona activa, titular obligatorio, saldo no
      negativo, stock suficiente, talonario único, total solo con lo aceptado)

### 14.2 Pendiente

- [ ] **Scripts SQL consolidados** (Oracle y PostgreSQL) en `scripts/`: tablas, secuencias, PK/FK,
      datos de catálogo (incluyendo los nombres de la sección 9.3). La carpeta no existe todavía.
- [ ] **Fase IV (requisito del curso):** instalar PostgreSQL, `ConexionPostgres`, `BackupEnCaliente`
      y una prueba que compare los conteos de cada tabla.
- [ ] **Capa View** (pantallas Swing, Jorge).
- [ ] Ajustes del backend para la vista: consultas con nombres resueltos (JOIN), búsquedas por
      texto, paginación y controller de catálogos.
- [ ] Corregir `nbproject/project.properties` (sección 4.1).
- [ ] Crear un esquema propio de Oracle para la entrega y limpiar los datos de prueba.
- [ ] Reemplazar `Main` (hoy arnés de pruebas manual; la última prueba cubre `ProductoController`)
      por la entrada real de la aplicación cuando exista la vista.
- [ ] Documentación del curso (Fases I a III, exposición): aporte del backend en la sección 16.

---

## 15. Decisiones y dudas abiertas

### 15.1 Decisiones tomadas

1. **`Albaran` y los talonarios** forman parte del modelo (el enunciado los describe).
2. **`Hortaliza` no es una entidad aparte:** son `Producto` con su `CategoriaProducto`.
3. **El albarán no modifica `Producto.stock`.** El stock es entero y cuenta unidades de insumos;
   el albarán trabaja con kilos decimales y es un documento de entrega y pago.
4. **Precio histórico:** `DetallePrestamo.precioUnitario` guarda el precio del día (desnormalización
   intencional). `Prestamo.monto` en los de consumo es la suma de sus detalles (dato derivado almacenado).
5. **Saldos como historial:** el saldo vigente es la fila más reciente de `saldos`.
6. **Catálogos por nombre:** los controllers dependen de que existan `ACTIVO`, `INACTIVO`, `SOCIO`
   y `CONSUMO_PRODUCTOS` (sección 9.3).

### 15.2 Dudas abiertas

1. **Pago al socio por un albarán:** ¿se abona a su cuenta, se paga en efectivo o solo se
   documenta? El enunciado no lo detalla.
2. **Abonos a préstamos:** no están definidos; hoy un préstamo solo cambia de estatus.
3. **Fase IV:** confirmar con el catedrático que PostgreSQL cubre "Firebird u otra base open source".
4. **Fechas de entrega** de cada fase (la tabla del enunciado está en blanco).
5. El enunciado habla de **importación** de productos pero describe lo que los socios entregan y
   la cooperativa exporta; los nombres de categoría viven en el catálogo de BD y el código no
   depende de ellos.

---

## 16. Lecciones aprendidas (material para "Consejos en la implementación")

| Problema                                         | Causa                                                      | Solución                                                 |
| ------------------------------------------------ | ---------------------------------------------------------- | -------------------------------------------------------- |
| `ORA-02289` secuencia no existe                  | La secuencia está en otro usuario/esquema                  | Todo el equipo usa el mismo esquema y los mismos scripts |
| `ORA-00001` en la llave primaria                 | Secuencia desfasada tras insertar datos con ids fijos      | Recrearla con `START WITH` máximo + 1                    |
| `ORA-00001` en DNI, talonario o número de cuenta | Valor repetido en una columna `UNIQUE`                     | Validar antes en el controller y mostrar mensaje claro   |
| `ORA-02291`                                      | La fila padre (sede, tipo, estatus) no existe              | Cargar los catálogos antes de los datos                  |
| `ORA-02292`                                      | Se borró una fila que otras tablas referencian             | Cambiar el estatus en lugar de borrar                    |
| `ORA-12519` listener sin manejador               | Demasiadas conexiones físicas abiertas y cerradas seguidas | Pool de conexiones en `ConexionOracle`                   |
| Datos a medias tras un error                     | Oracle confirma lo pendiente al cerrar la conexión         | `rollback` explícito ante cualquier excepción            |
| Caracteres raros (`Conexi�n`) en la consola      | Codificación de la salida de NetBeans                      | Solo afecta la consola; no cambia los datos              |

**Propuesta de buena práctica para los anexos del curso:** _transacciones atómicas con
rollback_. Evidencia ya disponible en las salidas de prueba: un préstamo con stock insuficiente
y un albarán con talonario repetido son rechazados sin dejar registros a medias. Otras buenas
prácticas con evidencia: `PreparedStatement` contra inyección SQL, credenciales fuera del
código y pool de conexiones.

---

## 17. Instrucciones para agentes de IA

- Responder y comentar el código **en español**.
- Código compatible con **Java 8**; respetar la estructura de paquetes de la sección 2.
- No agregar dependencias nuevas sin avisar y sin ponerlas en `lib/` con ruta relativa.
- Dinero siempre `BigDecimal`; fechas con `java.util.Date`.
- Todo SQL con `PreparedStatement` y recursos cerrados con try-with-resources.
- No subir credenciales ni modificar `nbproject/project.properties` con rutas absolutas.
- No cambiar nombres o tipos de las clases `model` sin avisar: BD y vista dependen de ellas.
- Los **15 DAO y los controllers** ya están implementados: **no reescribirlos desde cero**.
  Para extender uno, copiar el patrón existente (`COLUMNAS`, `siguienteId` con `seq_<tabla>`,
  sobrecarga con `Connection`, `mapear`).
- La vista solo llama a **controllers**, nunca a DAOs, y captura `NegocioException`.
- Toda operación que toque varias tablas va en una transacción con `rollback` ante cualquier
  excepción (patrón de la sección 9.6).
- `ConexionOracle` es un pool: usar siempre try-with-resources y no guardar conexiones en campos.
- El PDF del albarán **no** es requisito del curso; el backup en caliente a PostgreSQL **sí** (sección 11).
- Actualizar este archivo (sección 14 y sección 18) cuando cambie la arquitectura,
  las dependencias o las convenciones.

---

## 18. Historial de Cambios

| Fecha      | Cambio                                                                                             | Autor     |
| ---------- | -------------------------------------------------------------------------------------------------- | --------- |
| 2026-10-01 | Creación inicial del documento AGENTS.MD                                                           | Agente IA |
| 2026-10-01 | Fijado `javac.source/target=1.8` por compatibilidad con `ojdbc14.jar`                              | Agente IA |
| 2026-10-01 | Rutas de JAR relativas a `lib/`                                                                    | Agente IA |
| 2026-10-01 | `ConexionOracle` lee credenciales de `config.properties`                                           | Agente IA |
| 2026-10-01 | Entidades rediseñadas según el documento de trabajo (15 clases, catálogos); se elimina `Hortaliza` | Agente IA |
| 2026-10-01 | Estado real verificado contra el código: 15 model y 15 DAO implementados                           | Agente IA |
| 2026-10-01 | Documentados los 15 DAO, sus métodos propios y los patrones que siguen                             | Agente IA |
| 2026-10-01 | Documentada la lógica de negocio dentro de `model` (`calcularTotal`, `getSubtotal`)                | Agente IA |
| 2026-10-01 | Advertido que `nbproject/project.properties` real tenía `javac.source=26` y rutas absolutas        | Agente IA |
| 2026-10-01 | `ConexionOracle` pasa de una conexión nueva por llamada a un pool (por `ORA-12519`)                | Agente IA |
| 2026-10-01 | Añadida `CuentaDAO.bloquear` (`FOR UPDATE`) para movimientos de saldo                              | Agente IA |
| 2026-10-01 | Nueva sección 9: capa Controller (7 clases), reglas de negocio y patrón de transacción             | Agente IA |
| 2026-10-01 | Nueva sección 1.3: requisitos del curso (fases, puntos, Fase IV, entregables)                      | Agente IA |
| 2026-10-01 | Nueva sección 11: diseño de la migración y el backup en caliente a PostgreSQL                      | Agente IA |
| 2026-10-01 | Decisiones de diseño (albarán sin stock, precio histórico) y dudas abiertas actualizadas           | Agente IA |
| 2026-10-01 | Nueva sección 16: lecciones aprendidas y propuesta de buena práctica; se quita el estado de Git    | Agente IA |

---

> **Nota**: Este documento debe actualizarse cada vez que se realicen cambios significativos en la arquitectura, dependencias o convenciones del proyecto.
