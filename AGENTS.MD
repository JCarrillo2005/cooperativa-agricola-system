# AGENTS.MD — Sistema Cooperativa Agrícola

> **Documento de contexto técnico y arquitectura** para desarrolladores y agentes de IA.
> Última actualización: 2026-10-01

---

## 1. Descripción General del Proyecto

| Campo                        | Valor                                   |
| ---------------------------- | --------------------------------------- |
| **Nombre**                   | Sistema Cooperativa Agrícola "Tiendita" |
| **Tipo**                     | Aplicación de escritorio Java SE        |
| **IDE**                      | Apache NetBeans 31                      |
| **Lenguaje**                 | Java SE (source/target 1.8 / 11)        |
| **Base de datos principal**  | Oracle Database 10g (XE)                |
| **Base de datos secundaria** | PostgreSQL                              |
| **Arquitectura**             | MVC / DAO (Data Access Object)          |
| **Build system**             | Apache Ant                              |
| **Patrón de UI**             | Java Swing (JFrames / GUI Builder)      |

### Propósito

Sistema de gestión para una cooperativa agrícola que administra:

- **Personas** (cooperativistas, clientes)
- **Cuentas** (ahorros)
- **Préstamos**
- **Productos** (semillas, abonos, insecticidas)
- **Hortalizas**
- **Albaranes** (documentos de entrega)

---

## 2. Estructura del Repositorio

```
SistemaCooperativaAgricola/
├── src/
│   └── com/cooperativa/
│       ├── config/          # Configuración de conexión a BD
│       │   └── ConexionOracle.java
│       ├── dao/             # Data Access Objects (CRUD) — POR IMPLEMENTAR
│       ├── main/            # Punto de entrada
│       │   └── Main.java
│       ├── model/           # Entidades POJO — POR IMPLEMENTAR
│       ├── utils/           # Utilidades (PDF, backup) — POR IMPLEMENTAR
│       └── view/            # Interfaces gráficas Swing — POR IMPLEMENTAR
├── lib/                     # Dependencias JAR
│   ├── ojdbc14.jar          # Driver JDBC Oracle
│   └── postgresql-42.7.13.jar  # Driver JDBC PostgreSQL
├── nbproject/               # Configuración NetBeans
│   ├── project.xml          # Definición del proyecto
│   ├── project.properties   # Propiedades de compilación
│   ├── build-impl.xml       # Implementación de build
│   └── private/             # Configuración privada (ignorada por Git)
├── build/                   # Directorio de compilación (ignorado)
├── build.xml                # Script Ant para build
├── manifest.mf              # Manifest del JAR
├── README.md                # Documentación general
└── AGENTS.MD                # Este archivo
```

---

## 3. Arquitectura del Sistema

### 3.1 Patrón MVC / DAO

```
┌─────────────────────────────────────────────────────┐
│                    VIEW (Swing)                      │
│              JFrames / JDialogs                      │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                   CONTROLLER                         │
│         (Lógica de negocio / Servicios)              │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                     DAO                              │
│         (Data Access Objects - CRUD)                 │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                   DATABASE                            │
│         Oracle 10g / PostgreSQL                      │
└─────────────────────────────────────────────────────┘
```

### 3.2 Capas del Sistema

| Capa       | Paquete                  | Responsabilidad                                      | Estado             |
| ---------- | ------------------------ | ---------------------------------------------------- | ------------------ |
| **Config** | `com.cooperativa.config` | Conexión a base de datos                             | ✅ Implementada    |
| **Model**  | `com.cooperativa.model`  | Entidades POJO (Persona, Cuenta, Prestamo, Producto) | ❌ Por implementar |
| **DAO**    | `com.cooperativa.dao`    | Operaciones CRUD y transacciones SQL                 | ❌ Por implementar |
| **View**   | `com.cooperativa.view`   | Interfaces gráficas (JFrames)                        | ❌ Por implementar |
| **Utils**  | `com.cooperativa.utils`  | Reportes PDF, Hot Backup                             | ❌ Por implementar |
| **Main**   | `com.cooperativa.main`   | Punto de entrada de la aplicación                    | ✅ Implementada    |

---

## 4. Configuración del Proyecto

### 4.1 Propiedades de Compilación (`nbproject/project.properties`)

```properties
# Versión de Java (compatible con ojdbc14.jar)
javac.source=1.8
javac.target=1.8
source.encoding=UTF-8

# Clase principal
main.class=com.cooperativa.main.Main

# Dependencias (classpath)
javac.classpath=\
    ${file.reference.ojdbc14.jar}:\
    ${file.reference.postgresql-42.7.13.jar}

# Referencias a JARs (rutas relativas dentro del proyecto)
file.reference.ojdbc14.jar=lib/ojdbc14.jar
file.reference.postgresql-42.7.13.jar=lib/postgresql-42.7.13.jar

# Directorios
build.dir=build
dist.dir=dist
dist.jar=${dist.dir}/SistemaCooperativaAgricola.jar
src.dir=src
test.src.dir=test
```

#### ⚠️ Por qué `javac.source=1.8` y no una versión superior

El driver **`ojdbc14.jar` es legado** (diseñado originalmente para Java 1.4/5). Compilar con
Java 26 rompería la ejecución por cambios profundos en la JVM, `reflection` y el manejo de TLS.

| Versión de Java  | ¿Compatible con `ojdbc14.jar`?    |
| ---------------- | --------------------------------- |
| **Java 8 (1.8)** | ✅ Recomendada para este proyecto |
| **Java 11**      | ✅ Alternativa válida             |
| Java 17+         | ⚠️ Riesgo de incompatibilidad     |
| Java 26          | ❌ No usar                        |

**Regla:** no subir `javac.source` / `javac.target` por encima de 1.8 sin, antes,
migrar a un driver Oracle moderno (`ojdbc8.jar` / `ojdbc11.jar` o superior).

#### ⚠️ Por qué las rutas de los JAR son relativas

Las referencias apuntan a `lib/` dentro del proyecto y **no** a rutas absolutas del
dispositivo de un solo desarrollador.

Rutas absolutas del tipo `C:\Users\Julio Cesar\Downloads\...` rompen la compilación
en NetBeans para cualquier otro miembro del equipo
tras clonar el repositorio, porque esas carpetas no existen en sus máquinas.

Si algún JAR cambia de nombre o versión, actualizar **las dos cosas**:

1. El archivo dentro de `lib/`
2. Las líneas `file.reference.*` de `nbproject/project.properties`

### 4.2 Conexión a Base de Datos

#### Oracle 10g (Principal)

| Parámetro  | Valor                                 |
| ---------- | ------------------------------------- |
| URL        | `jdbc:oracle:thin:@localhost:1521:XE` |
| Usuario    | `USUARIO`                             |
| Contraseña | `CONTRASEÑA`                          |
| Driver     | `oracle.jdbc.driver.OracleDriver`     |

#### PostgreSQL (Secundaria)

| Parámetro | Valor                    |
| --------- | ------------------------ |
| Driver    | `org.postgresql.Driver`  |
| JAR       | `postgresql-42.7.13.jar` |

---

## 5. Dependencias

### 5.1 Librerías JAR

| JAR                      | Ubicación | Descripción            |
| ------------------------ | --------- | ---------------------- |
| `ojdbc14.jar`            | `lib/`    | Driver JDBC Oracle 10g |
| `postgresql-42.7.13.jar` | `lib/`    | Driver JDBC PostgreSQL |

### 5.2 Tecnologías Utilizadas

| Tecnología      | Versión          | Uso                                             |
| --------------- | ---------------- | ----------------------------------------------- |
| Java SE         | 1.8 / 11         | Lenguaje principal (compatible con ojdbc14.jar) |
| Apache NetBeans | 31               | IDE de desarrollo                               |
| Oracle Database | 10g (10.2.0.1.0) | Base de datos principal                         |
| PostgreSQL      | -                | Base de datos secundaria                        |
| JDBC            | -                | Conexión a bases de datos                       |
| Apache Ant      | -                | Sistema de build                                |
| Java Swing      | -                | Framework de UI                                 |

---

## 6. Patrones de Diseño

### 6.1 Patrones Identificados

| Patrón                       | Implementación                         | Estado                       |
| ---------------------------- | -------------------------------------- | ---------------------------- |
| **DAO (Data Access Object)** | Capa `dao/` para CRUD                  | ❌ No implementado           |
| **MVC**                      | Separación Model-View-Controller       | ⚠️ Parcial (solo estructura) |
| **Singleton (implícito)**    | `ConexionOracle` con conexión estática | ✅ Implementado básico       |
| **POJO**                     | Clases en `model/`                     | ❌ No implementado           |

### 6.2 Clase `ConexionOracle.java`

```java
public class ConexionOracle {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "system";
    private static final String PASSWORD = "password";
    private static Connection conexion = null;

    public static Connection getConexion() {
        if (conexion == null || conexion.isClosed()) {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conexion = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return conexion;
    }
}
```

**Características:**

- Conexión única estática (Singleton básico)
- Lazy initialization
- Manejo de excepciones básico
- ⚠️ **Credenciales hardcodeadas** (no recomendado para producción)

---

## 7. Entidades del Dominio

### 7.1 Entidades Planificadas

| Entidad        | Descripción                    | Atributos Esperados                                |
| -------------- | ------------------------------ | -------------------------------------------------- |
| **Persona**    | Cooperativistas y clientes     | id, nombre, apellido, dni, dirección, teléfono     |
| **Cuenta**     | Cuentas de ahorro              | id, persona_id, saldo, fecha_apertura              |
| **Prestamo**   | Préstamos otorgados            | id, persona_id, monto, tasa_interes, fecha, estado |
| **Producto**   | Semillas, abonos, insecticidas | id, nombre, descripción, precio, stock             |
| **Hortalizas** | Productos agrícolas            | id, nombre, variedad, temporada                    |
| **Albaranes**  | Documentos de entrega          | id, fecha, persona_id, productos, total            |

### 7.2 Diagrama de Relaciones (Conceptual)

```
Persona (1) ───< Cuenta (N)
Persona (1) ───< Prestamo (N)
Persona (1) ───< Albaran (N)
Albaran (N) ───> Producto (N)  [tabla puente]
```

---

## 8. Guías de Desarrollo

### 8.1 Convenciones de Nomenclatura

| Elemento   | Convención       | Ejemplo                               |
| ---------- | ---------------- | ------------------------------------- |
| Clases     | PascalCase       | `PersonaDAO`, `ConexionOracle`        |
| Métodos    | camelCase        | `obtenerPersona()`, `guardarCuenta()` |
| Variables  | camelCase        | `nombreCliente`, `listaProductos`     |
| Constantes | UPPER_SNAKE_CASE | `URL`, `USER`, `PASSWORD`             |
| Paquetes   | minúsculas       | `com.cooperativa.dao`                 |

### 8.2 Estructura de un DAO

```java
package com.cooperativa.dao;

import com.cooperativa.model.Persona;
import com.cooperativa.config.ConexionOracle;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonaDAO {

    // CREATE
    public boolean guardar(Persona persona) { ... }

    // READ
    public Persona obtenerPorId(int id) { ... }
    public List<Persona> obtenerTodos() { ... }

    // UPDATE
    public boolean actualizar(Persona persona) { ... }

    // DELETE
    public boolean eliminar(int id) { ... }
}
```

### 8.3 Estructura de una Entidad (POJO)

```java
package com.cooperativa.model;

public class Persona {
    private int id;
    private String nombre;
    private String apellido;
    private String dni;
    private String direccion;
    private String telefono;

    // Constructores
    public Persona() {}

    public Persona(int id, String nombre, String apellido, String dni) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    // ... resto de getters/setters

    @Override
    public String toString() {
        return nombre + " " + apellido;
    }
}
```

### 8.4 Manejo de Conexiones

```java
// Patrón recomendado para operaciones de BD
Connection conn = null;
PreparedStatement stmt = null;
ResultSet rs = null;

try {
    conn = ConexionOracle.getConexion();
    String sql = "SELECT * FROM personas WHERE id = ?";
    stmt = conn.prepareStatement(sql);
    stmt.setInt(1, id);
    rs = stmt.executeQuery();

    if (rs.next()) {
        // Procesar resultados
    }
} catch (SQLException e) {
    e.printStackTrace();
} finally {
    // Cerrar recursos en orden inverso
    if (rs != null) try { rs.close(); } catch (SQLException e) {}
    if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
    if (conn != null) try { conn.close(); } catch (SQLException e) {}
}
```

---

## 9. Comandos de Build y Ejecución

### 9.1 Compilar el Proyecto

```bash
# Desde la raíz del proyecto (usando Ant)
ant compile

# O compilar todo incluyendo JAR
ant jar
```

### 9.2 Ejecutar la Aplicación

```bash
# Desde NetBeans: botón derecho > Run File
# O desde línea de comandos:
java -cp "build/classes;lib/ojdbc14.jar;lib/postgresql-42.7.13.jar" com.cooperativa.main.Main
```

### 9.3 Limpiar y Reconstruir

```bash
ant clean
ant jar
```

---

## 10. Archivos de Configuración

### 10.1 `nbproject/project.properties`

- Configuración de compilación
- Definición de la clase principal
- Referencias a JARs de dependencias (rutas relativas)

### 10.2 `nbproject/project.xml`

- Definición del proyecto NetBeans
- Raíces de código fuente y pruebas

### 10.3 `build.xml`

- Script Ant para build personalizado
- Usa `nbproject/build-impl.xml` como base

### 10.4 `manifest.mf`

- Manifest del JAR
- `Main-Class` se agrega automáticamente

### 10.5 `.gitignore`

```
build/
dist/
nbproject/private/
*.class
```

---

## 11. Estado del Proyecto

### 11.1 Completado

- [x] Estructura de carpetas del proyecto
- [x] Clase `Main.java` (punto de entrada)
- [x] Clase `ConexionOracle.java` (conexión JDBC)
- [x] Configuración del proyecto NetBeans
- [x] Dependencias JAR configuradas (rutas relativas)
- [x] README.md con documentación básica

### 11.2 Pendiente

- [ ] Capa Model (entidades POJO)
- [ ] Capa DAO (CRUD para cada entidad)
- [ ] Capa View (interfaces Swing)
- [ ] Capa Utils (reportes PDF, Hot Backup)
- [ ] Scripts SQL de creación de tablas
- [ ] Lógica de negocio completa
- [ ] Pruebas unitarias
- [ ] Manejo de transacciones
- [ ] Validaciones de datos
- [ ] Manejo de errores robusto

---

## 12. Notas Importantes para Desarrolladores

### 12.1 Seguridad

- ⚠️ **Las credenciales de la base de datos están hardcodeadas** en `ConexionOracle.java`
- **Recomendación**: Mover a un archivo de configuración externo o variables de entorno
- **Recomendación**: Usar `PreparedStatement` para prevenir SQL Injection

### 12.2 Manejo de Recursos

- **Siempre cerrar** `Connection`, `Statement` y `ResultSet` en bloque `finally`
- Considerar usar **try-with-resources** (Java 7+)
- No mantener conexiones abiertas innecesariamente

### 12.3 Transacciones

- Para operaciones que requieren múltiples INSERT/UPDATE:
  ```java
  conn.setAutoCommit(false);
  try {
      // múltiples operaciones
      conn.commit();
  } catch (SQLException e) {
      conn.rollback();
  }
  ```

### 12.4 UI (Swing)

- Usar `SwingUtilities.invokeLater()` para iniciar la UI
- Considerar usar `SwingWorker` para operaciones de BD en segundo plano
- Separar la lógica de presentación de la lógica de negocio

---

## 13. Equipo de Desarrollo

| Integrante                          | Rol |
| ----------------------------------- | --- |
| (Ver README.md para lista completa) | -   |

---

## 14. Referencias

- [Documentación oficial de Java SE](https://docs.oracle.com/en/java/)
- [JDBC Tutorial](https://docs.oracle.com/javase/tutorial/jdbc/)
- [NetBeans IDE Documentation](https://netbeans.apache.org/)
- [Oracle 10g Documentation](https://docs.oracle.com/cd/B19306_01/server.102/b14220/toc.htm)

---

## 15. Historial de Cambios

| Fecha      | Cambio                                                                      | Autor     |
| ---------- | --------------------------------------------------------------------------- | --------- |
| 2026-10-01 | Creación inicial del documento AGENTS.MD                                    | Agente IA |
| 2026-10-01 | Añadida sección 0 (Alcance) para dejar claro que es Java SE, no C#/.NET/WPF | Agente IA |
| 2026-10-01 | Fijado `javac.source/target=1.8` por compatibilidad con `ojdbc14.jar`       | Agente IA |
| 2026-10-01 | Sustituidas rutas absolutas de JAR por rutas relativas a `lib/`             | Agente IA |

---

> **Nota**: Este documento debe actualizarse cada vez que se realicen cambios significativos en la arquitectura, dependencias o convenciones del proyecto.
