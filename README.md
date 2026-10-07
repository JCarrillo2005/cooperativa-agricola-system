# Sistema de Gestión Integral - Cooperativa Agrícola

[![Java](https://img.shields.io/badge/Java-8%20(JDK%201.8)-orange.svg)](https://www.oracle.com/java/)
[![Oracle Database](https://img.shields.io/badge/Oracle_DB-10g_XE-red.svg)](https://www.oracle.com/database/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16.x_(Fase_IV)-blue.svg)](https://www.postgresql.org/)
[![Architecture](https://img.shields.io/badge/Arquitectura-MVC%20%7C%20DAO%20%7C%20Controllers-lightgrey.svg)]()
[![License](https://img.shields.io/badge/Académico-UMG_Base_de_Datos_I-green.svg)]()

Sistema informático desarrollado como proyecto de curso para **Base de Datos I** (Universidad Mariano Gálvez de Guatemala). Resuelve la problemática integral de administración operativa, financiera y de acopio de una **Cooperativa Agrícola con presencia en múltiples sedes a nivel nacional**, enfocada en brindar soporte a pequeños y medianos agricultores.

---

## 1. Problemática del Negocio y Solución Propuesta

Las cooperativas agrícolas en el país enfrentan una problemática descentralizada: pequeños productores agropecuarios en diversas regiones (sedes) requieren financiamiento oportuno, fomento al ahorro y mecanismos justos de comercialización de sus cosechas sin intermediarios abusivos.

El sistema da respuesta a esta problemática mediante cuatro pilares operativos:

1. **Servicios Financieros y Créditos Agrícolas:**
   - Préstamos en efectivo o hipotecarios con control de plazos y tasas.
   - **Préstamos de consumo de insumos agrícolas:** entrega de semillas, abonos, fertilizantes e insecticidas con cálculo automático y descuento inmediato de inventario en bodega.
2. **Cuentas de Ahorro y Manejo Mancomunado:**
   - Cuentas de ahorro y monetarias con registro de múltiples asociados por cuenta (titulares y cotitulares).
   - Trazabilidad y control histórico estricto de saldos mediante movimientos transaccionales atómicos protegidos contra concurrencia.
3. **Acopio, Recepción y Control de Calidad de Cosechas:**
   - Recepción de hortalizas, legumbres y frutas entregadas por los socios mediante talonarios oficiales de entrega.
   - Clasificación técnica por control de calidad: separación entre producto procesado (aceptado) y producto rechazado.
   - Incentivo económico: el producto aceptado se remunera a un **mejor precio que el promedio de mercado**.
4. **Liquidación Transparente mediante Albaranes:**
   - Emisión de albaranes que detallan la entrega, el volumen evaluado y el monto neto a liquidar al socio (calculado exclusivamente sobre la cantidad aceptada).

---

## 2. Integrantes del Equipo

| Nombre | Rol Principal | Responsabilidad |
| :--- | :--- | :--- |
| **Julio César Carrillo García** | **Coordinador & Backend Developer** | Arquitectura general, Config (pools), Model (POJOs), DAO, Controller, Utils (Backup) |
| **José Adrián** | **DB Admin & Modelado** | Modelado E-R, DDL/DML Oracle 10g y PostgreSQL, normalización y verificación |
| **Jorge Leonidas** | **Frontend Developer (Swing/GUI)** | Diseño de interfaz de usuario, JFrames, JPanels y enlace a controllers |
| **Nahomi López** | **Documentadora Líder & QA** | Pruebas de integración, matriz de pruebas de integridad y redacción de informe |
| **Marvin Muñoz** | **Documentador & Diseñador** | Resumen ejecutivo, infografía de arquitectura y presentación |

---

## 3. Examen Profundo: Matriz de Cumplimiento de Entidades y Requisitos

A continuación se presenta el contraste riguroso entre los requerimientos solicitados para la problemática y la implementación actual en el repositorio:

| Entidad / Requisito Solicitado | Descripción del Negocio | Estado en BD | Estado en Backend (Model/DAO/Controller) | Estado en Vista (Swing) | ¿Cumple lo Solicitado? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Entidad / Requisito Solicitado | Descripción del Negocio | Estado en BD | Estado en Backend (Model/DAO/Controller) | Estado en Vista (Swing) | ¿Cumple lo Solicitado? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Personas** | Socios y empleados de la cooperativa. | Tabla `personas` (Oracle & PG) | `Persona`, `PersonaDAO`, `PersonaController` | [`PanelPersonas`](src/com/cooperativa/view/PanelPersonas.java) (CRUD y combos) | **100% Cumplido** |
| **Estatus** | Activo, inactivo, pagado, etc. | Tabla `estatus` (Oracle & PG) | `Estatus`, `EstatusDAO`, `CatalogosController` | Combos dinámicos en UI | **100% Cumplido** |
| **Tipo de Personas** | Empleado, socio u otros. | Tabla `tipos_persona` (Oracle & PG) | `TipoPersona`, `TipoPersonaDAO`, `CatalogosController` | Combos dinámicos en UI | **100% Cumplido** |
| **Sedes (Cedes)** | Sedes en diferentes lugares del país. | Tabla `sedes` (Oracle & PG) | `Sede`, `SedeDAO`, `CatalogosController` | Combos dinámicos en UI | **100% Cumplido** |
| **Préstamos** | Registro formal del crédito (monto, tasa, fechas). | Tabla `prestamos` (Oracle & PG) | `Prestamo`, `PrestamoDAO`, `PrestamoController` | [`PanelPrestamos`](src/com/cooperativa/view/PanelPrestamos.java) | **100% Cumplido** |
| **Tipo Préstamo** | Efectivo, hipotecario, consumo de productos. | Tabla `tipos_prestamo` (Oracle & PG) | `TipoPrestamo`, `TipoPrestamoDAO`, `CatalogosController` | Combos y selector dinámico | **100% Cumplido** |
| **Detalle Préstamo** | Detalle de insumos entregados (semillas, abonos). | Tabla `detalles_prestamo` (Oracle & PG) | `DetallePrestamo`, `DetallePrestamoDAO` | Tabla dinámica en [`PanelPrestamos`](src/com/cooperativa/view/PanelPrestamos.java) | **100% Cumplido** |
| **Productos** | Catálogo de insumos agrícolas y cosechas. | Tabla `productos` (Oracle & PG) | `Producto`, `ProductoDAO`, `ProductoController` | [`PanelProductos`](src/com/cooperativa/view/PanelProductos.java) | **100% Cumplido** |
| **Categoría Productos** | Insumos para socios vs. productos de acopio/exportación. | Tabla `categorias_producto` (Oracle & PG) | `CategoriaProducto`, `CategoriaProductoDAO`, `CatalogosController` | Combos dinámicos en UI | **100% Cumplido** |
| **Cuentas** | Cuentas asociadas a una o varias personas. | Tablas `cuentas` y `cuentas_personas` (puente M:N) | `Cuenta`, `CuentaPersona`, `CuentaDAO`, `CuentaPersonaDAO`, `CuentaController` | [`PanelCuentas`](src/com/cooperativa/view/PanelCuentas.java) (apertura y cotitulares) | **100% Cumplido** |
| **Tipo Cuenta** | Ahorro, monetaria. | Tabla `tipos_cuenta` (Oracle & PG) | `TipoCuenta`, `TipoCuentaDAO`, `CatalogosController` | Combos dinámicos en UI | **100% Cumplido** |
| **Saldos** | Historial de saldos y saldo actual por cuenta. | Tabla `saldos` (Oracle & PG) | `Saldo`, `SaldoDAO`, transacciones con bloqueo `FOR UPDATE` | Depósitos, retiros y auditoría en [`PanelCuentas`](src/com/cooperativa/view/PanelCuentas.java) | **100% Cumplido** |
| **Albarán (Envíos/Talonarios)** | Documento generado tras la entrega de cosecha. | Tabla `albaranes` (Oracle & PG) | `Albaran`, `AlbaranDAO`, `AlbaranController` | [`PanelAlbaranes`](src/com/cooperativa/view/PanelAlbaranes.java) | **100% Cumplido** |
| **Detalle Albarán (Calidad)** | Cantidad aceptada, cantidad rechazada y precio fijado. | Tabla `detalles_albaran` (Oracle & PG) | `DetalleAlbaran`, `DetalleAlbaranDAO` (cálculo solo sobre lo aceptado) | Tabla de calidad en [`PanelAlbaranes`](src/com/cooperativa/view/PanelAlbaranes.java) | **100% Cumplido** |
| **Fase IV (Open Source & Backup)** | Migración y backup en caliente a PostgreSQL. | Scripts PostgreSQL y BD de respaldo | `ConexionPostgres`, `BackupEnCaliente`, `EsquemaRespaldo` | Verificado por CLI y test | **100% Cumplido** |

---

## 4. Estado Actual: Funcionalidades Implementadas

1. **Capa de Configuración y Pool de Conexiones:**
   - Pool de conexiones físico liviano en [`ConexionOracle`](src/com/cooperativa/config/ConexionOracle.java) para erradicar el error `ORA-12519`.
   - Externalización de credenciales mediante [`config.properties`](config.properties.example).
   - Soporte JDBC a PostgreSQL mediante [`ConexionPostgres`](src/com/cooperativa/config/ConexionPostgres.java).

2. **Capa de Negocio y Controladores:**
   - 6 controladores de negocio (`PersonaController`, `CuentaController`, `PrestamoController`, `AlbaranController`, `ProductoController`, `CatalogosController`).
   - Patrón de **transacciones atómicas con rollback explícito**: consistencia garantizada en apertura de cuentas, créditos de insumos con descuento de inventario y albaranes.
   - Traducción de códigos Oracle a mensajes entendibles mediante [`NegocioException`](src/com/cooperativa/controller/NegocioException.java).
   - **Cierre de Ciclo Operacional:** Liquidación directa del total aceptado de un albarán acreditándolo a la cuenta de ahorro del socio (`liquidarAlbaranEnCuenta`) y abonos/amortizaciones a préstamos (`registrarAbono`).

3. **Capa de Interfaz Gráfica (Swing GUI 100% Operativa):**
   - [`VentanaPrincipal.java`](src/com/cooperativa/view/VentanaPrincipal.java): Marco principal con navegación por pestañas (`JTabbedPane`) y refresco dinámico de datos.
   - [`PanelPersonas.java`](src/com/cooperativa/view/PanelPersonas.java): Registro, actualización, desactivación y consulta de socios y empleados con combos sincronizados.
   - [`PanelProductos.java`](src/com/cooperativa/view/PanelProductos.java): Catálogo de productos e insumos agrícolas, reabastecimiento de bodega, ajuste de precios y filtro de stock crítico (≤ 5).
   - [`PanelCuentas.java`](src/com/cooperativa/view/PanelCuentas.java): Apertura de cuentas de ahorro/monetarias, asociación de cotitulares, depósitos, retiros y auditoría histórica de saldos en tiempo real.
   - [`PanelPrestamos.java`](src/com/cooperativa/view/PanelPrestamos.java): Otorgamiento de préstamos en efectivo, hipotecarios y créditos de insumos agrícolas (descuento automático de bodega), con registro de abonos y cuotas.
   - [`PanelAlbaranes.java`](src/com/cooperativa/view/PanelAlbaranes.java): Talonarios de entrega de cosechas, inspección de control de calidad (kilos aceptados vs rechazados), cálculo del total y liquidación inmediata con acreditación bancaria al socio.

4. **Fase IV: Migración y Backup en Caliente:**
   - Módulo [`BackupEnCaliente`](src/com/cooperativa/utils/BackupEnCaliente.java) con ejecución CLI para respaldo transaccional consistente (`SET TRANSACTION READ ONLY` en Oracle, copiado en lotes `addBatch` a PostgreSQL y recuperación de contingencia).
   - Scripts DDL y DML espejo entre Oracle 10g y PostgreSQL con 73 pruebas de integridad documentadas en [`scripts/VALIDACION_BD.md`](scripts/VALIDACION_BD.md).

---

## 5. Requisitos para Poner en Marcha el Sistema (Despliegue Local)

Para ejecutar el software de principio a fin en cualquier estación de trabajo:

1. **Configurar Credenciales Locales:**
   Crear el archivo `config.properties` en la raíz del proyecto tomando como plantilla `config.properties.example` con el usuario/password de Oracle y PostgreSQL.
2. **Cargar la Base de Datos Oracle:**
   Ejecutar los scripts `01_schema_oracle.sql` y `02_initial_data.sql` en Oracle Database 10g XE.
3. **Ejecutar la Aplicación:**
   Correr `com.cooperativa.main.Main` desde NetBeans (**F6**) o por terminal. La aplicación verificará la conexión a la base de datos y abrirá la interfaz gráfica interactiva.

---

## 6. Arquitectura del Repositorio

```text
SistemaCooperativaAgricola/
├── src/
│   └── com/cooperativa/
│       ├── config/              # Conexión y Pool JDBC
│       │   ├── ConexionOracle.java      # Pool de conexiones físicas a Oracle 10g
│       │   └── ConexionPostgres.java    # Driver y conexión a PostgreSQL
│       ├── model/               # 15 Entidades del Dominio (POJOs)
│       │   ├── Persona.java, Cuenta.java, Prestamo.java, Albaran.java, Producto.java...
│       ├── dao/                 # 15 DAOs (Acceso a Datos y SQL)
│       │   ├── PersonaDAO.java, CuentaDAO.java, PrestamoDAO.java, AlbaranDAO.java...
│       ├── controller/          # 5 Controladores de Reglas de Negocio
│       │   ├── PersonaController.java, CuentaController.java, PrestamoController.java...
│       │   ├── Catalogos.java, NegocioException.java
│       ├── utils/               # Fase IV: Backup en Caliente y Migración
│       │   ├── BackupEnCaliente.java, EsquemaRespaldo.java
│       ├── view/                # Frontend Java Swing (JFrames y JPanels)
│       │   ├── VentanaPrincipal.java
│       │   └── PanelPersonas.java
│       └── main/                # Entrada Principal del Sistema
│           └── Main.java
├── lib/                         # Drivers JDBC con rutas relativas
│   ├── ojdbc14.jar              # Driver Oracle 10g
│   └── postgresql-42.7.13.jar   # Driver PostgreSQL
├── scripts/                     # DDL/DML, verificación y documentación de BD
│   ├── 01_schema_oracle.sql, 02_initial_data.sql, 03_drop_all.sql
│   ├── 04_schema_postgresql.sql, 05_initial_data_postgresql.sql, 06_drop_all_postgresql.sql
│   ├── 07_verify_oracle.sql, 08_verify_postgresql.sql
│   ├── README_BD.md             # Guía paso a paso de base de datos
│   └── VALIDACION_BD.md         # Bitácora de validación técnica
├── nbproject/                   # Metadatos del proyecto Apache NetBeans
├── config.properties.example    # Plantilla de credenciales (en Git)
├── config.properties            # Credenciales locales reales (ignorado por Git)
├── build.xml                    # Script de compilación Apache Ant
├── AGENTS.MD                    # Bitácora técnica y contratos de arquitectura
└── README.md                    # Documentación general del proyecto
```

---

## 7. Requisitos y Guía de Puesta en Marcha

### Prerrequisitos de Software
1. **Java Development Kit (JDK):** Java SE 8 (1.8).
2. **Entorno de Desarrollo (IDE):** Apache NetBeans (versión 21, 25, 26 o 31).
3. **Gestor de Compilación:** Apache Ant (integrado en NetBeans o CLI).
4. **Base de Datos Principal:** Oracle Database 10g Express Edition (XE 10.2.0.1.0) en puerto `1521`.
5. **Base de Datos Secundaria:** PostgreSQL 16.x en puerto `5432` (para Fase IV).

### Paso 1: Configurar la Base de Datos Oracle
1. Conectarse a SQL*Plus o SQL Developer con usuario administrador (`SYSTEM`).
2. Ejecutar los scripts en orden:
   ```sql
   @scripts/01_schema_oracle.sql
   @scripts/02_initial_data.sql
   ```
*(Instrucciones detalladas de usuarios y esquemas en [`scripts/README_BD.md`](scripts/README_BD.md)).*

### Paso 2: Configurar Credenciales Locales
Copiar el archivo [`config.properties.example`](config.properties.example) como `config.properties` en la raíz del proyecto y colocar sus credenciales locales:

```properties
db.url=jdbc:oracle:thin:@localhost:1521:XE
db.user=COOPERATIVA
db.password=TU_PASSWORD_AQUI

pg.url=jdbc:postgresql://localhost:5432/cooperativa_respaldo
pg.user=postgres
pg.password=TU_PASSWORD_AQUI
```

### Paso 3: Compilación y Ejecución

#### Desde Apache NetBeans:
1. Abrir el proyecto en NetBeans.
2. Limpiar y compilar con **F11** (`Clean and Build`).
3. Ejecutar la aplicación con **F6** (`Run Project`).

#### Desde Terminal (PowerShell / Bash):
```powershell
# Compilar con Ant
ant clean compile

# Ejecutar aplicación Swing
java -cp "build/classes;lib/ojdbc14.jar;lib/postgresql-42.7.13.jar" com.cooperativa.main.Main
```

#### Ejecutar Backup en Caliente (Fase IV por CLI):
```powershell
# Probar conexión a PostgreSQL
java -cp "build/classes;lib/ojdbc14.jar;lib/postgresql-42.7.13.jar" com.cooperativa.config.ConexionPostgres

# Ejecutar respaldo en caliente consistente
java -cp "build/classes;lib/ojdbc14.jar;lib/postgresql-42.7.13.jar" com.cooperativa.utils.BackupEnCaliente respaldar
```

---

## 8. Licencia y Contexto Académico

Proyecto desarrollado con fines académicos para el curso de **Base de Datos I**, Facultad de Ingeniería en Sistemas de Información, **Universidad Mariano Gálvez de Guatemala (UMG)**.
Prohibida su comercialización sin autorización de los autores.
