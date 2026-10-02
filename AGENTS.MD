# AGENTS.MD — Sistema de Gestión Integral · Cooperativa Agrícola

> **Documento de contexto técnico y arquitectura** para desarrolladores y agentes de IA.
> Última actualización: 2026-10-01

---

## 0. Alcance

- Proyecto **académico en equipo**.
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
| **Base de datos secundaria** | PostgreSQL (migración y Hot Backup)                |
| **Arquitectura**             | MVC / DAO                                          |
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

| Integrante                      | Rol                                                                      |
| ------------------------------- | ------------------------------------------------------------------------ |
| **Julio César Carrillo García** | Coordinador y desarrollador backend Java (model, dao, controller, utils) |
| **José Adrián**                 | Modelado y administración de BD (Oracle / PostgreSQL)                    |
| **Jorge Leonidas**              | Desarrollador frontend Java (Swing / GUI)                                |
| **Nahomi López**                | Documentadora líder y pruebas                                            |
| **Marvin Muñoz**                | Documentador y diseñador (resumen ejecutivo / infografía)                |

**Regla de trabajo:** el backend define el modelo y los contratos (clases `model`, firmas de
los DAO); la BD y la interfaz se adaptan a ellos. Cualquier cambio de nombres o tipos en
`model` debe avisarse al equipo.

---

## 2. Estructura del Repositorio

```
SistemaCooperativaAgricola/
├── src/
│   └── com/cooperativa/
│       ├── config/          # Conexión a BD
│       │   └── ConexionOracle.java
│       ├── model/           # Entidades POJO
│       ├── dao/             # Data Access Objects (CRUD y transacciones SQL)
│       ├── controller/      # Lógica de negocio / servicios (valida y coordina DAOs)
│       ├── utils/           # Reportes PDF (albaranes) y Hot Backup
│       ├── view/            # Interfaces Swing (JFrames / JDialogs)
│       └── main/
│           └── Main.java    # Punto de entrada
├── lib/
│   ├── ojdbc14.jar
│   └── postgresql-42.7.13.jar
├── nbproject/               # Configuración NetBeans (private/ se ignora)
├── build.xml
├── manifest.mf
├── config.properties.example  # Plantilla de credenciales (SÍ se sube a Git)
├── config.properties          # Credenciales reales (NO se sube a Git)
├── README.md
└── AGENTS.MD
```

> **Estado real de las carpetas:** existen `config/`, `model/`, `dao/` y `main/`.
> `controller/`, `view/`, `utils/` y `scripts/` **todavía no están creadas**.

---

## 3. Arquitectura del Sistema

### 3.1 Flujo MVC / DAO

```
VIEW (Swing)  →  CONTROLLER (lógica de negocio)  →  DAO (SQL / JDBC)  →  Oracle 10g / PostgreSQL
                                   ↑
                           MODEL (POJOs) viaja entre todas las capas
```

- La **vista** nunca habla con un DAO ni escribe SQL.
- El **controller** valida reglas (saldo suficiente, estatus activo, stock) y decide
  qué DAOs se llaman y dentro de qué transacción.
- El **DAO** solo traduce entre objetos `model` y SQL.

### 3.2 Estado por capa

| Capa           | Paquete                      | Responsabilidad                | Estado                    |
| -------------- | ---------------------------- | ------------------------------ | ------------------------- |
| **Config**     | `com.cooperativa.config`     | Fábrica de conexiones JDBC     | ✅ Implementada           |
| **Model**      | `com.cooperativa.model`      | 15 entidades POJO              | ✅ Implementada           |
| **DAO**        | `com.cooperativa.dao`        | CRUD y transacciones           | ✅ Implementada (15 DAOs) |
| **Controller** | `com.cooperativa.controller` | Reglas de negocio              | ❌ No existe aún          |
| **View**       | `com.cooperativa.view`       | Pantallas Swing                | ❌ No existe aún          |
| **Utils**      | `com.cooperativa.utils`      | PDF de albaranes, Hot Backup   | ❌ No existe aún          |
| **Main**       | `com.cooperativa.main`       | Prueba de conexión y albaranes | ✅ Implementada           |

> **Las carpetas `controller/`, `view/` y `utils/` aún no están creadas en disco.**
> `model/`, `dao/`, `config/` y `main/` son las únicas con código.

### 3.3 Por qué se implementaron `model` y `dao` antes que el resto

Es una decisión de la regla de trabajo del equipo (sección 1.2), no un descuido:

1. **`model` fija el contrato.** Las 15 clases POJO definen los nombres de campo y tipos
   que la BD y la Swing van a consumir. Si el modelo cambia después, se rompen el DDL,
   los `mapear()` de los DAO y las pantallas. Es la capa de menor riesgo para montar primero.
2. **`dao` cierra el circuito con la BD.** Los DAO fijan los **nombres de tabla y columna**
   (`personas.tipo_persona_id`, `detalles_albaran.cantidad_aceptada`, …). José necesita
   esas columnas exactas para escribir el DDL, así que los DAO son la especificación que
   consume el modelado de BD.
3. **`controller`, `view` y `utils` dependen de ambos.** El controller llama a firmas de
   DAO ya decididas; la vista consume POJOs ya cerrados. Se pueden escribir al final
   sin bloquear a nadie.
4. **Se validó en conjunto.** `Main` ya exercises el camino completo del albarán
   (encabezado + detalles en una transacción), así que el backend tiene una prueba de
   humo real antes de existir la interfaz.

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

> ⚠️ **El archivo real `nbproject/project.properties` todavía NO coincide con este bloque.**
> Al momento de esta revisión (2026-10-01) contiene:
>
> ```properties
> javac.source=26
> javac.target=26
> file.reference.ojdbc14.jar=C:\\oraclexe\\app\\oracle\\product\\10.2.0\\server\\jdbc\\lib\\ojdbc14.jar
> file.reference.postgresql-42.7.13.jar=C:\\Users\\Julio Cesar\\Downloads\\postgresql-42.7.13.jar
> ```
>
> Los 4 valores de arriba son la **configuración objetivo**; el código actual compila
> con ellos en la máquina de Julio César, pero **rompe al clonar el repo** en la de
> cualquier otro integrante. Corregir antes de volver a hacer commit
> (ver la sección 4.2).

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

PostgreSQL: driver `org.postgresql.Driver`, JAR `postgresql-42.7.13.jar`.

---

## 5. Dependencias

| JAR                      | Ubicación | Descripción            |
| ------------------------ | --------- | ---------------------- |
| `ojdbc14.jar`            | `lib/`    | Driver JDBC Oracle 10g |
| `postgresql-42.7.13.jar` | `lib/`    | Driver JDBC PostgreSQL |

| Tecnología      | Versión          | Uso                       |
| --------------- | ---------------- | ------------------------- |
| Java SE         | 1.8              | Lenguaje principal        |
| Apache NetBeans | 31               | IDE                       |
| Oracle Database | 10g (10.2.0.1.0) | BD principal              |
| PostgreSQL      | -                | BD secundaria / migración |
| Apache Ant      | -                | Build                     |
| Java Swing      | -                | UI                        |

---

## 6. Capa Config: `ConexionOracle`

Fábrica de conexiones. **No es un singleton:** cada llamada a `getConexion()` devuelve una
**conexión nueva**, que quien la pide debe cerrar (try-with-resources). Así cada operación
y cada transacción tienen su propia conexión.

```java
public static Connection getConexion() throws SQLException   // abre una conexión nueva
public static boolean probarConexion()                       // true si conecta
```

- Lee `config.properties` y carga el driver en un bloque `static`.
- Si falta el archivo o el driver, lanza `ExceptionInInitializerError` con un mensaje claro.

Uso básico:

```java
try (Connection conn = ConexionOracle.getConexion()) {
    // usar conn
} catch (SQLException e) {
    // manejar error
}
```

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

| Java                        | Oracle 10g                   | Notas                                                       |
| --------------------------- | ---------------------------- | ----------------------------------------------------------- |
| `int id`                    | `NUMBER(10)` PK              | Secuencia `SEQ_<TABLA>` + trigger (10g no tiene `IDENTITY`) |
| `String`                    | `VARCHAR2(n)`                |                                                             |
| `BigDecimal` (dinero)       | `NUMBER(12,2)`               | Nunca `double` para dinero                                  |
| `BigDecimal` (tasa)         | `NUMBER(5,2)`                |                                                             |
| `BigDecimal` (cantidad, kg) | `NUMBER(12,2)`               | En `DetalleAlbaran`                                         |
| `java.util.Date`            | `DATE`                       | En el DAO: `new java.sql.Date(f.getTime())`                 |
| `boolean`                   | `NUMBER(1)` (0/1)            | Oracle 10g no tiene `BOOLEAN` en SQL                        |
| `campoId` (FK)              | `NUMBER(10)` + `FOREIGN KEY` | `personaId` → columna `persona_id`                          |

- Los nombres de columna son el campo en `snake_case` (`fechaApertura` → `fecha_apertura`).
- Tablas en plural y `snake_case`: `personas`, `cuentas`, `cuentas_personas`, `saldos`,
  `prestamos`, `detalles_prestamo`, `productos`, `albaranes`, `detalles_albaran`,
  `sedes`, `estatus`, `tipos_persona`, `tipos_cuenta`, `tipos_prestamo`, `categorias_producto`.
- Oracle trata la cadena vacía `''` como `NULL`; tenerlo en cuenta en validaciones.
- Paginación con `ROWNUM` (10g no tiene `LIMIT` ni `FETCH FIRST`).

### 7.4 Lógica de negocio dentro de `model` (excepción consciente)

Tres POJOs llevan comportamiento, no solo datos. Es intencional: son cálculos que
dependen **solo** del propio objeto, sin BD ni reglas de negocio, así que se pueden
usar igual desde un DAO, el controller o la vista.

| Método                            | Clase            | Qué hace                                |
| --------------------------------- | ---------------- | --------------------------------------- |
| `calcularTotal()`                 | `Albaran`        | Suma los subtotales de los detalles     |
| `getSubtotal()`                   | `DetalleAlbaran` | `cantidadAceptada × precioUnitario`     |
| `agregarDetalle(DetalleAlbaran)`  | `Albaran`        | Añade una línea a la lista de detalles  |
| `agregarDetalle(DetallePrestamo)` | `Prestamo`       | Añade una línea de producto al préstamo |

El resto de las 15 clases son POJOs planos: constructor vacío, constructor útil,
getters/setters y `toString()`. `Prestamo` y `Albaran` son los únicos con lista de
detalles, inicializada a `new ArrayList<...>()` para que nunca sea `null`.

`DetalleAlbaran` tiene un constructor de 4 parámetros, usado por `Main` al armar el
albarán de prueba:

```java
public DetalleAlbaran(int productoId, BigDecimal cantidadAceptada,
                      BigDecimal cantidadRechazada, BigDecimal precioUnitario)
```

**El total del albarán se calcula solo con lo aceptado**, nunca con la cantidad
entregada: lo rechazado no se le paga al socio.

---

## 8. Capa DAO: los 15 DAOs implementados

`com.cooperativa.dao` tiene **un DAO por entidad de `model`**. Los 15 compilan y están
probados contra Oracle mediante `Main`.

| DAO                    | Tabla                 | Métodos propios además del CRUD estándar                                                 |
| ---------------------- | --------------------- | ---------------------------------------------------------------------------------------- |
| `SedeDAO`              | `sedes`               | —                                                                                        |
| `EstatusDAO`           | `estatus`             | `obtenerPorNombre(String)`                                                               |
| `TipoPersonaDAO`       | `tipos_persona`       | —                                                                                        |
| `TipoCuentaDAO`        | `tipos_cuenta`        | —                                                                                        |
| `TipoPrestamoDAO`      | `tipos_prestamo`      | —                                                                                        |
| `CategoriaProductoDAO` | `categorias_producto` | —                                                                                        |
| `PersonaDAO`           | `personas`            | `obtenerPorDni()`, `obtenerPorTipo()`, `cambiarEstatus()`                                |
| `CuentaDAO`            | `cuentas`             | `obtenerPorNumero()`, `obtenerPorPersona()` (JOIN con `cuentas_personas`)                |
| `CuentaPersonaDAO`     | `cuentas_personas`    | `agregar()`, `cambiarTitular()`, `quitar()`, `obtenerPorCuenta()`, `obtenerPorPersona()` |
| `SaldoDAO`             | `saldos`              | `registrar()`, `obtenerActual()` (con `ROWNUM = 1`), `obtenerHistorial()`                |
| `PrestamoDAO`          | `prestamos`           | `cambiarEstatus()`, `obtenerPorIdConDetalles()`, `obtenerPorPersona()`                   |
| `DetallePrestamoDAO`   | `detalles_prestamo`   | `guardar()`, `obtenerPorPrestamo()`                                                      |
| `ProductoDAO`          | `productos`           | `obtenerPorCategoria()`, **`descontarStock()`**, **`aumentarStock()`**                   |
| `AlbaranDAO`           | `albaranes`           | `obtenerPorNumeroTalonario()`, `obtenerPorIdConDetalles()`, `obtenerPorPersona()`        |
| `DetalleAlbaranDAO`    | `detalles_albaran`    | `guardar()`, `obtenerPorAlbaran()`                                                       |

### 8.1 Patrones que sigue TODOS los DAO (verificados en el código)

**a) Constante `COLUMNAS` y SQL sin `SELECT *`.**
Los DAO principales (`Persona`, `Cuenta`, `Prestamo`, `Producto`, `Albaran`) declaran
`private static final String COLUMNAS` y la concatenan en cada consulta. Los DAO de
catálogo y de línea de detalle escriben la lista de columnas en el literal.

**b) Sobrecarga con `Connection` para transacciones.**
Este es el patrón central del proyecto. La versión sin `Connection` abre y cierra la suya
con try-with-resources (commit automático); la versión con `Connection` **no abre ni cierra**
nada, para poder encadenarla dentro de la transacción de otro DAO:

```java
public boolean guardar(Persona p) throws SQLException {
    try (Connection conn = ConexionOracle.getConexion()) {
        return guardar(p, conn);   // delega
    }
}

public boolean guardar(Persona p, Connection conn) throws SQLException { ... }
```

**c) ID con secuencia Oracle, nunca con trigger ni `IDENTITY`.**
Cada DAO tiene un `siguienteId(Connection)` privado que pide el valor a la secuencia:

```java
private int siguienteId(Connection conn) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement("SELECT seq_personas.NEXTVAL FROM dual");
         ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
    }
}
```

Convenciones de nombre de secuencia: `seq_personas`, `seq_cuentas`, `seq_saldos`,
`seq_prestamos`, `seq_detalles_prestamo`, `seq_productos`, `seq_albaranes`,
`seq_detalles_albaran`, `seq_sedes`, `seq_estatus`, `seq_tipos_persona`,
`seq_tipos_cuenta`, `seq_tipos_prestamo`, `seq_categorias_producto`.

**d) Método privado `mapear(ResultSet)`.**
Cada DAO tiene un único `mapear` que construye el POJO. `AlbaranDAO`, `PrestamoDAO` y
`PersonaDAO` lo usan en todas sus consultas; los catálogos también.

**e) Fechas de alta con `SYSDATE`, no desde Java.**
`personas.fecha_registro`, `albaranes.fecha`, `prestamos.fecha`, `saldos.fecha` y
`cuentas.fecha_apertura` se llenan en el `INSERT` con `SYSDATE`, por eso esas columnas no
llegan como parámetro `?`.

**f) `null` cuando no hay resultado.**
Todos los `obtenerPorId` devuelven `null` si el `ResultSet` está vacío (no lanzan excepción).

**g) Fecha de lectura con `getTimestamp`.**
Los `mapear` leen fechas con `rs.getTimestamp(...)` y las guardan en `java.util.Date`
(conserva la hora). En `PrestamoDAO`, al escribir, se usa
`new java.sql.Date(f.getTime())` y `setNull(..., Types.DATE)` cuando es `null`.

**h) Booleanos como `NUMBER(1)`.**
`CuentaPersonaDAO` mapea `titular` con `rs.getInt("titular") == 1` y escribe
`cp.isTitular() ? 1 : 0`.

### 8.2 Notas de diseño por DAO

**`ProductoDAO.descontarStock(int, int, Connection)`** — descuenta stock de forma atómica
con una sola sentencia y devuelve `false` si no hay inventario, sin modificar nada:

```java
String sql = "UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?";
```

Solo existe en la versión con `Connection`: siempre se usa dentro de la transacción de un
préstamo de consumo. `aumentarStock` sí suma sin condición.

**`SaldoDAO.obtenerActual(int, Connection)`** — el saldo vigente es el registro más
reciente, con el patrón de subconsulta que sí funciona en Oracle 10g (no hay
`ORDER BY` dentro de un `INLINE VIEW` sin `FETCH FIRST`):

```sql
SELECT id, cuenta_id, monto, fecha FROM (
  SELECT id, cuenta_id, monto, fecha FROM saldos
  WHERE cuenta_id = ? ORDER BY fecha DESC, id DESC
) WHERE ROWNUM = 1
```

Existe con y sin `Connection` porque hay que **leer el saldo antes de modificarlo**
dentro de la misma transacción.

**`CuentaDAO.obtenerPorPersona(int)`** — resuelve el many-to-many de la tabla puente
`cuentas_personas` con un `JOIN` explícito. No consulta "solo titulares": trae cualquier
persona asociada.

**Borrados físicos con `ORA-02292`.** `eliminar()` hace `DELETE` directo. Si la fila ya
está referenciada, Oracle lanza `ORA-02292` (violación de FK) y eso **es intencional**: los
Javadoc de `SedeDAO`, `CuentaDAO`, `ProductoDAO` y `EstatusDAO` lo advertían. Para dar de
baja sin borrar está `cambiarEstatus(id, estatusId)`, disponible en `PersonaDAO`,
`CuentaDAO` y `PrestamoDAO`.

### 8.3 Encabezado + detalle (composición)

`AlbaranDAO.obtenerPorIdConDetalles(int)` y `PrestamoDAO.obtenerPorIdConDetalles(int)`
llaman a su `obtenerPorId` y luego inyectan la lista con
`new DetalleAlbaranDAO().obtenerPorAlbaran(id)`. El DAO de detalle no sabe nada del
encabezado: solo usa `d.getAlbaranId()`, que debe venir ya asignado por el llamador.

`DetalleAlbaranDAO` y `DetallePrestamoDAO` **no implementan `eliminar()` ni
`actualizar()`**: son líneas inmutables de su encabezado. Si una línea cambia, se rehace el
albarán o el préstamo.

### 8.4 Qué NO hay todavía en la capa DAO

- **Sin transacciones dentro de los DAO.** La transacción la abre y cierra el llamador
  (`Main` o el futuro controller) y pasa la `Connection` a cada DAO.
- **Sin paginación.** No hay métodos paginados ni uso de `ROWNUM` para listados, solo para
  `obtenerActual`. Cuando se necesiten, agregarlos sin romper las firmas actuales.
- **Sin búsqueda de texto** (`LIKE` por nombre, DNI parcial, talonario parcial).
- **Sin operaciones en lote** (importar/exportar Catálogo de productos, partidas de albarán).
- **Sin soporte PostgreSQL.** Todo el SQL es Oracle 10g.

---

## 9. Guías de Desarrollo

### 9.1 Nomenclatura

| Elemento   | Convención       | Ejemplo                           |
| ---------- | ---------------- | --------------------------------- |
| Clases     | PascalCase       | `PersonaDAO`, `ConexionOracle`    |
| Métodos    | camelCase        | `obtenerPorId()`, `guardar()`     |
| Variables  | camelCase        | `nombreCliente`, `listaProductos` |
| Constantes | UPPER_SNAKE_CASE | `ARCHIVO_CONFIG`, `COLUMNAS`      |
| Paquetes   | minúsculas       | `com.cooperativa.dao`             |

Alumnos de español, no de inglés: los métodos de negocio usan
`obtenerPorId`, `obtenerTodos`, `cambiarEstatus`, `descontarStock`, `registrar`,
`agregarDetalle`. Los comentarios y los mensajes de error, en español.

### 9.2 Cómo escribir un DAO nuevo (checklist)

1. Un DAO por entidad de `model`, mismo nombre con sufijo `DAO`.
2. `private static final String COLUMNAS` con la lista explícita de columnas.
3. `siguienteId(Connection)` con `SELECT seq_<tabla>.NEXTVAL FROM dual`.
4. Par de métodos `guardar(X)` y `guardar(X, Connection)`, con delegación entre ambos.
5. `obtenerPorId`, `obtenerTodos`, `actualizar`, `eliminar` con `throws SQLException`.
6. `private X mapear(ResultSet)` como único constructor de POJO desde BD.
7. Todo `SELECT` con columnas explícitas y `ORDER BY` estable.
8. Fechas de alta con `SYSDATE` en el SQL; fechas de lectura con `getTimestamp`.
9. Recursos con try-with-resources, incluido el `ResultSet` en la misma cadena.
10. Javadoc en español en los métodos con comportement no obvio (transaccional,
    borrado que falla por FK, descuenta condicional de stock).

### 9.3 Patrón de consulta (ejemplo real, `SedeDAO`)

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
- Cada operación abre y cierra su conexión con try-with-resources.
- En las listas, el `ResultSet` va en la misma cadena del `try`.

### 9.4 Transacciones

Las operaciones que tocan varias tablas deben ser **atómicas**. El patrón ya validado
está en `Main.registrarAlbaran(Albaran)`: encabezado + todas sus líneas, o nada.

```java
try (Connection conn = ConexionOracle.getConexion()) {
    conn.setAutoCommit(false);
    try {
        albaranDAO.guardar(albaran, conn);
        for (DetalleAlbaran d : albaran.getDetalles()) {
            d.setAlbaranId(albaran.getId());   // el DAO de detalle no lo adivina
            detalleDAO.guardar(d, conn);
        }
        conn.commit();
    } catch (SQLException e) {
        conn.rollback();
        throw e;
    }
}
```

Casos que requieren transacción: albarán, préstamo de consumo
(encabezado + detalle + `descontarStock`), apertura de cuenta (cuenta + titulares +
saldo inicial), movimientos de saldo.

**Al escribir detalles dentro de una transacción, el llamador asigna la FK**
(`d.setAlbaranId(a.getId())`) después de guardar el encabezado.

### 9.5 Reglas de negocio a validar en el Controller

- Solo una persona con estatus **activo** puede recibir préstamos o abrir cuentas.
- Un préstamo de **consumo de productos** debe tener al menos un `DetallePrestamo` y
  descontar stock con `descontarStock`; si devuelve `false`, no hay inventario y la
  transacción debe revertirse.
- Un préstamo de **efectivo / hipotecario** no lleva detalle.
- El **total del albarán** se calcula solo con la cantidad **aceptada**
  (`DetalleAlbaran.getSubtotal()` → `Albaran.calcularTotal()`).
- Una cuenta debe tener al menos una persona titular.
- `numero_talonario` es único: `Main` prueba el duplicado a propósito y espera
  `SQLException`.

### 9.6 UI (Swing)

- Iniciar la UI con `SwingUtilities.invokeLater()`.
- Usar `SwingWorker` para consultas a BD, para no congelar la ventana.
- La vista solo llama al Controller; no contiene SQL ni reglas de negocio.
- La vista carga catálogos con los `obtenerTodos()` de los DAO simples.

### 9.7 Seguridad

- Credenciales solo en `config.properties` (ignorado por Git).
- Solo `PreparedStatement`.
- No imprimir contraseñas ni la cadena de conexión completa en logs.

---

## 10. Comandos de Build y Ejecución

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

## 11. Flujo de trabajo con Git

- `main` siempre debe compilar. No se trabaja directamente sobre `main`.
- **Una rama por tarea**, con prefijo `feature/`: `feature/model`, `feature/persona-dao`,
  `feature/scripts-sql`, `feature/vista-personas`.
- Mensajes de commit cortos y en español: `Agrega clases del paquete model`.
- Integración mediante **Pull Request** revisado por otro integrante.
- Antes de seguir trabajando, traer lo nuevo de `main` a la rama propia.
- Antes de commitear, revisar `git diff`: NetBeans puede modificar `nbproject/project.properties`
  con rutas absolutas; no subir ese cambio.

```bash
git checkout main && git pull origin main
git checkout -b feature/nombre-de-la-tarea
git add . && git commit -m "Descripción del cambio"
git push -u origin feature/nombre-de-la-tarea
```

---

## 12. Estado del Proyecto

### 12.1 Completado

- [x] Estructura de carpetas del proyecto
- [x] Configuración NetBeans y dependencias JAR
- [x] `ConexionOracle` con `config.properties` (conexión nueva por llamada)
- [x] `config.properties` + `config.properties.example` (el primero en `.gitignore`)
- [x] `Main` con prueba de conexión (`probarConexion()`)
- [x] Diseño de las entidades del dominio a partir del documento de trabajo
- [x] **Capa Model: 15 clases POJO** en `com.cooperativa.model`
- [x] **Capa DAO: 15 clases** en `com.cooperativa.dao` (CRUD + operaciones por entidad)
- [x] Transacciones probadas en `Main`: albarán completo con encabezado + detalles
- [x] Prueba de `numero_talonario` duplicado (debe lanzar `SQLException`)
- [x] `ProductoDAO.descontarStock` / `aumentarStock` para stock transaccional
- [x] `SaldoDAO.obtenerActual` con patrón `ROWNUM` válido en Oracle 10g

### 12.2 Pendiente

- [ ] **Scripts SQL**: la carpeta `scripts/` **no existe todavía**. Necesita tablas,
      secuencias `seq_*`, PK/FK, y datos de catálogos. Los DAO ya definieron los nombres
      de tabla y columna exactos (sección 8).
- [ ] **Capa Controller** con las reglas de la sección 9.5
- [ ] **Transacciones** en producción: apertura de cuenta (cuenta + titulares + saldo
      inicial) y préstamo de consumo (encabezado + detalle + `descontarStock`)
- [ ] **Capa View** (pantallas Swing) — las carpetas `controller/`, `view/`, `utils/` no existen
- [ ] **Utils**: PDF de albaranes y Hot Backup
- [ ] **Paginación** y búsquedas de texto en los DAO
- [ ] Migración / pruebas con PostgreSQL (Fase IV)
- [ ] Pruebas automatizadas y validaciones de datos

### 12.3 Estado de Git (verificado)

- Rama actual: `main`. Los 15 DAO están **sin commitear** (`??` en `git status`).
- `Main.java` tiene cambios locales sin commitear (`M`).
- Solo existe `main`; **no hay todavía ramas `feature/`**.
- Último commit: `7d33867 feat: integración de la clase PersonaDAO`.

---

## 13. Dudas abiertas (confirmar con el equipo)

1. `Albarán` y las entregas por talonario aparecen en la descripción del negocio pero no
   en la lista de entidades del documento. Se incluyeron; confirmar que BD y documentación
   también las tengan.
2. El documento habla de "importación" de productos agrícolas pero describe lo que los socios
   **entregan** y la cooperativa **exporta**. Se nombró la categoría `PRODUCTO_EXPORTACION`;
   confirmar el término.
3. `Hortaliza` ya no es una entidad aparte: las hortalizas son `Producto` con su
   `CategoriaProducto`.

---

## 14. Instrucciones para agentes de IA

- Responder y comentar el código **en español**.
- Código compatible con **Java 8**; respetar la estructura de paquetes de la sección 2.
- No agregar dependencias nuevas sin avisar y sin ponerlas en `lib/` con ruta relativa.
- Dinero siempre `BigDecimal`; fechas con `java.util.Date`.
- Todo SQL con `PreparedStatement` y recursos cerrados con try-with-resources.
- No subir credenciales ni modificar `nbproject/project.properties` con rutas absolutas.
- No cambiar nombres o tipos de las clases `model` sin avisar: BD y vista dependen de ellas.
- Los 15 DAO de la sección 8 ya están implementados: **no reescribirlos desde cero.**
  Para extender un DAO, copiar el patrón existente (`COLUMNAS`, `siguienteId` con
  `seq_<tabla>`, sobrecarga con `Connection`, `mapear`).
- Para escribir SQL nuevo, respetar lo que los DAO ya exigen: nombres de tabla en
  `snake_case` plural, secuencias `seq_<tabla>`, fechas de alta con `SYSDATE`.
- Actualizar este archivo (sección 12 y sección 15) cuando cambie la arquitectura,
  las dependencias o las convenciones.

---

## 15. Historial de Cambios

| Fecha      | Cambio                                                                                             | Autor     |
| ---------- | -------------------------------------------------------------------------------------------------- | --------- |
| 2026-10-01 | Creación inicial del documento AGENTS.MD                                                           | Agente IA |
| 2026-10-01 | Fijado `javac.source/target=1.8` por compatibilidad con `ojdbc14.jar`                              | Agente IA |
| 2026-10-01 | Rutas de JAR relativas a `lib/`                                                                    | Agente IA |
| 2026-10-01 | `ConexionOracle` lee credenciales de `config.properties`; sin conexión estática                    | Agente IA |
| 2026-10-01 | Entidades rediseñadas según el documento de trabajo (15 clases, catálogos); se elimina `Hortaliza` | Agente IA |
| 2026-10-01 | Añadidos Controller, mapeo Oracle, reglas de negocio, flujo Git y dudas abiertas                   | Agente IA |
| 2026-10-01 | Estado real verificado contra el código: 15 model y 15 DAO implementados                           | Agente IA |
| 2026-10-01 | Nueva sección 8 con los 15 DAO, sus métodos propios y los 8 patrones que siguen                    | Agente IA |
| 2026-10-01 | Nueva sección 3.3 con la justificación de por qué model y dao van primero                          | Agente IA |
| 2026-10-01 | Documentada la lógica de negocio en `model` (`calcularTotal`, `getSubtotal`)                       | Agente IA |
| 2026-10-01 | Marcado que `scripts/` no existe y que las carpetas `controller/`, `view/`, `utils/` faltan        | Agente IA |
| 2026-10-01 | Advertido que `nbproject/project.properties` real aún tiene `javac.source=26` y rutas absolutas    | Agente IA |
| 2026-10-01 | Registrado el estado de Git: 15 DAO sin commitear, sin ramas `feature/`                            | Agente IA |

---

> **Nota**: Este documento debe actualizarse cada vez que se realicen cambios significativos en la arquitectura, dependencias o convenciones del proyecto.
