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
├── scripts/                 # DDL/DML para Oracle 10g y PostgreSQL
├── nbproject/               # Configuración NetBeans (private/ se ignora)
├── build.xml
├── manifest.mf
├── config.properties.example  # Plantilla de credenciales (SÍ se sube a Git)
├── config.properties          # Credenciales reales (NO se sube a Git)
├── README.md
└── AGENTS.MD
```

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

| Capa           | Paquete                      | Responsabilidad              | Estado             |
| -------------- | ---------------------------- | ---------------------------- | ------------------ |
| **Config**     | `com.cooperativa.config`     | Fábrica de conexiones JDBC   | ✅ Implementada    |
| **Model**      | `com.cooperativa.model`      | 15 entidades POJO            | 🔄 En progreso     |
| **DAO**        | `com.cooperativa.dao`        | CRUD y transacciones         | ❌ Por implementar |
| **Controller** | `com.cooperativa.controller` | Reglas de negocio            | ❌ Por implementar |
| **View**       | `com.cooperativa.view`       | Pantallas Swing              | ❌ Por implementar |
| **Utils**      | `com.cooperativa.utils`      | PDF de albaranes, Hot Backup | ❌ Por implementar |
| **Main**       | `com.cooperativa.main`       | Entrada y prueba de conexión | ✅ Implementada    |

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

---

## 8. Guías de Desarrollo

### 8.1 Nomenclatura

| Elemento   | Convención       | Ejemplo                           |
| ---------- | ---------------- | --------------------------------- |
| Clases     | PascalCase       | `PersonaDAO`, `ConexionOracle`    |
| Métodos    | camelCase        | `obtenerPorId()`, `guardar()`     |
| Variables  | camelCase        | `nombreCliente`, `listaProductos` |
| Constantes | UPPER_SNAKE_CASE | `ARCHIVO_CONFIG`                  |
| Paquetes   | minúsculas       | `com.cooperativa.dao`             |

### 8.2 Estructura de un DAO

Un DAO por entidad principal (`PersonaDAO`, `CuentaDAO`, `PrestamoDAO`, `ProductoDAO`,
`AlbaranDAO`, y DAOs simples para los catálogos). Métodos estándar:

```java
public class PersonaDAO {
    public boolean guardar(Persona p) throws SQLException { ... }
    public Persona obtenerPorId(int id) throws SQLException { ... }
    public List<Persona> obtenerTodos() throws SQLException { ... }
    public boolean actualizar(Persona p) throws SQLException { ... }
    public boolean eliminar(int id) throws SQLException { ... }
}
```

Cuando una operación forma parte de una transacción mayor, el DAO ofrece una **sobrecarga
que recibe la `Connection`** (`guardar(Persona p, Connection conn)`), y esa versión no la abre
ni la cierra.

### 8.3 Patrón de consulta

```java
public Persona obtenerPorId(int id) throws SQLException {
    String sql = "SELECT * FROM personas WHERE id = ?";
    try (Connection conn = ConexionOracle.getConexion();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, id);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapear(rs);   // método privado ResultSet -> Persona
            }
        }
    }
    return null;
}
```

- **Siempre** `PreparedStatement` (evita SQL Injection). Nunca concatenar valores en el SQL.
- Cada DAO tiene un método privado `mapear(ResultSet)` que construye el POJO.
- Cada operación abre y cierra su conexión con try-with-resources.

### 8.4 Transacciones

Las operaciones que tocan varias tablas deben ser **atómicas**. Caso típico: registrar un
albarán (encabezado + detalles + actualización de stock/saldo del socio).

```java
try (Connection conn = ConexionOracle.getConexion()) {
    conn.setAutoCommit(false);
    try {
        albaranDAO.guardar(albaran, conn);          // encabezado + detalles
        // otros DAOs con la misma conn: stock, saldo, etc.
        conn.commit();
    } catch (SQLException e) {
        conn.rollback();
        throw e;
    }
}
```

Casos que requieren transacción: albarán, préstamo de consumo (préstamo + detalle + stock),
movimientos de saldo.

### 8.5 Reglas de negocio a validar en el Controller

- Solo una persona con estatus **activo** puede recibir préstamos o abrir cuentas.
- Un préstamo de **consumo de productos** debe tener al menos un `DetallePrestamo` y descontar stock.
- Un préstamo de **efectivo / hipotecario** no lleva detalle.
- El **total del albarán** se calcula solo con la cantidad **aceptada** (`DetalleAlbaran.getSubtotal()`).
- Una cuenta debe tener al menos una persona titular.

### 8.6 UI (Swing)

- Iniciar la UI con `SwingUtilities.invokeLater()`.
- Usar `SwingWorker` para consultas a BD, para no congelar la ventana.
- La vista solo llama al Controller; no contiene SQL ni reglas de negocio.

### 8.7 Seguridad

- Credenciales solo en `config.properties` (ignorado por Git).
- Solo `PreparedStatement`.
- No imprimir contraseñas ni la cadena de conexión completa en logs.

---

## 9. Comandos de Build y Ejecución

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

## 10. Flujo de trabajo con Git

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

## 11. Estado del Proyecto

### 11.1 Completado

- [x] Estructura de carpetas del proyecto
- [x] Configuración NetBeans y dependencias JAR (rutas relativas)
- [x] `ConexionOracle` con `config.properties` (conexión nueva por llamada)
- [x] `Main` con prueba de conexión (`probarConexion()`)
- [x] Diseño de las entidades del dominio a partir del documento de trabajo

### 11.2 En progreso

- [ ] Capa Model: 15 clases POJO (compilar y subir a `feature/model`)

### 11.3 Pendiente

- [ ] Scripts SQL de creación de tablas, secuencias y datos de catálogos (`scripts/`)
- [ ] Capa DAO (empezar por `PersonaDAO`, luego catálogos, cuentas, productos, préstamos, albaranes)
- [ ] Capa Controller con las reglas de la sección 8.5
- [ ] Transacciones (albarán, préstamo de consumo, saldos)
- [ ] Capa View (pantallas Swing)
- [ ] Utils: PDF de albaranes y Hot Backup
- [ ] Migración / pruebas con PostgreSQL (Fase IV)
- [ ] Pruebas y validaciones de datos

---

## 12. Dudas abiertas (confirmar con el equipo)

1. `Albarán` y las entregas por talonario aparecen en la descripción del negocio pero no
   en la lista de entidades del documento. Se incluyeron; confirmar que BD y documentación
   también las tengan.
2. El documento habla de "importación" de productos agrícolas pero describe lo que los socios
   **entregan** y la cooperativa **exporta**. Se nombró la categoría `PRODUCTO_EXPORTACION`;
   confirmar el término.
3. `Hortaliza` ya no es una entidad aparte: las hortalizas son `Producto` con su
   `CategoriaProducto`.

---

## 13. Instrucciones para agentes de IA

- Responder y comentar el código **en español**.
- Código compatible con **Java 8**; respetar la estructura de paquetes de la sección 2.
- No agregar dependencias nuevas sin avisar y sin ponerlas en `lib/` con ruta relativa.
- Dinero siempre `BigDecimal`; fechas con `java.util.Date`.
- Todo SQL con `PreparedStatement` y recursos cerrados con try-with-resources.
- No subir credenciales ni modificar `nbproject/project.properties` con rutas absolutas.
- No cambiar nombres o tipos de las clases `model` sin avisar: BD y vista dependen de ellas.
- Actualizar este archivo (sección 11 y sección 14) cuando cambie la arquitectura,
  las dependencias o las convenciones.

---

## 14. Historial de Cambios

| Fecha      | Cambio                                                                                             | Autor     |
| ---------- | -------------------------------------------------------------------------------------------------- | --------- |
| 2026-10-01 | Creación inicial del documento AGENTS.MD                                                           | Agente IA |
| 2026-10-01 | Fijado `javac.source/target=1.8` por compatibilidad con `ojdbc14.jar`                              | Agente IA |
| 2026-10-01 | Rutas de JAR relativas a `lib/`                                                                    | Agente IA |
| 2026-10-01 | `ConexionOracle` lee credenciales de `config.properties`; sin conexión estática                    | Agente IA |
| 2026-10-01 | Entidades rediseñadas según el documento de trabajo (15 clases, catálogos); se elimina `Hortaliza` | Agente IA |
| 2026-10-01 | Añadidos Controller, mapeo Oracle, reglas de negocio, flujo Git y dudas abiertas                   | Agente IA |

---

> **Nota**: Este documento debe actualizarse cada vez que se realicen cambios significativos en la arquitectura, dependencias o convenciones del proyecto.
