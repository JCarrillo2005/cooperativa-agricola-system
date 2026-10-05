# Base de datos de la Cooperativa Agrícola

Esta guía prepara una copia local para el equipo: Oracle XE 10g como base principal, PostgreSQL como destino de migración y respaldo, y Java 8 para ejecutar el proyecto. Los nombres de tablas, columnas y secuencias siguen los 15 DAO existentes. Cada integrante usa sus propias contraseñas y el mismo nombre de esquema `COOPERATIVA` en su instancia local.

## Archivos y orden de ejecución

| Archivo | Función |
| --- | --- |
| `01_schema_oracle.sql` | Oracle: 15 tablas, 15 PK, 19 FK, restricciones e índices, y 14 secuencias. |
| `02_initial_data.sql` | Oracle: catálogos y datos ficticios para pruebas. |
| `03_drop_all.sql` | Oracle: elimina exclusivamente los objetos del proyecto; pide confirmación. |
| `04_schema_postgresql.sql` | PostgreSQL: estructura equivalente para migración y respaldo. |
| `05_initial_data_postgresql.sql` | PostgreSQL: mismos catálogos y datos ficticios. |
| `06_drop_all_postgresql.sql` | PostgreSQL: limpieza de los objetos del proyecto; pide confirmación. |
| `07_verify_oracle.sql` | Oracle: verifica estructura y rechazo de datos inválidos con los datos de prueba. |
| `08_verify_postgresql.sql` | PostgreSQL: verificación equivalente. |

Instalación de prueba: **Oracle 01 → 02 → 07**; **PostgreSQL 04 → 05 → 08**. Destino de un respaldo real: ejecutar **solo 04**, porque el respaldo reemplaza los datos de PostgreSQL. Destino de una recuperación: ejecutar **solo 01** en un esquema Oracle nuevo; la recuperación exige tablas vacías.

`cuentas_personas` usa PK compuesta `(cuenta_id, persona_id)` y no necesita secuencia. Los catálogos incluyen `ACTIVO`, `INACTIVO`, `SOCIO` y `CONSUMO_PRODUCTOS`, que los controllers buscan por nombre. Los scripts de datos crean 38 filas en las 15 tablas; los registros personales y financieros son ficticios.

## 1. Preparar herramientas en Windows

1. Clonar el repositorio y abrir una terminal en su carpeta raíz, donde están `build.xml`, `lib/` y `config.properties.example`:

   ```powershell
   git clone https://github.com/JCarrillo2005/cooperativa-agricola-system.git
   Set-Location cooperativa-agricola-system
   ```

2. Instalar el **JDK 8** acordado por el equipo, con `java.exe` y `javac.exe`. Verificar en la terminal que `java -version` y `javac -version` muestran `1.8` para compilar y ejecutar este proyecto.
3. Instalar NetBeans 31 con su runtime de IDE. NetBeans 31 necesita un JDK moderno para arrancar; el JDK que ejecuta el IDE es independiente del JDK del proyecto. Su [documentación oficial](https://netbeans.apache.org/front/main/download/nb31/) indica JDK 21, 25 o 26 para el IDE. Los instaladores mencionados allí pueden incluir ese runtime.
4. En NetBeans, registrar el JDK 8 en **Tools → Java Platforms → Add Platform**. Abrir el proyecto y seleccionarlo en **Properties → Libraries → Java Platform**, con formato fuente Java 8. Los archivos del repositorio usan `javac.source=1.8`, `javac.target=1.8` y las rutas relativas `lib/ojdbc14.jar` y `lib/postgresql-42.7.13.jar`.
5. Para compilar fuera de NetBeans, instalar Apache Ant y comprobar `ant -version`. No mover los JAR a una carpeta personal ni añadir rutas `C:\Users\...` a los archivos compartidos.

La [página oficial de pgJDBC](https://jdbc.postgresql.org/download/) identifica `42.7.13` como driver JDBC 4.2 para Java 8 o posterior. El JAR ya está en `lib/`; no hace falta descargar otra dependencia para la conexión Java.

## 2. Instalar Oracle XE 10g en una máquina limpia

Oracle 10g es una versión antigua. La [guía oficial XE 10g para Windows](https://docs.oracle.com/cd/B25329_01/doc/install.102/b25143/toc.htm) enumera Windows 2000, Server 2003 y XP de **32 bits**; no acredita Windows 10/11. En una máquina actual, usar la VM compatible preparada por la universidad/equipo o una instalación de laboratorio. Obtener el instalador legítimo de Oracle o de la institución; esta guía no asegura que siga disponible públicamente.

Dentro del entorno compatible:

1. Entrar con una cuenta administradora y ejecutar el instalador XE 10g suministrado. Elegir una ruta sin espacios; el valor tradicional es `C:\oraclexe`.
2. Aceptar la licencia y conservar el listener `1521` si está libre. Registrar la contraseña administrativa de `SYS`/`SYSTEM` en un gestor privado.
3. Completar la instalación. Revisar en **Servicios** que `OracleServiceXE` y el listener XE estén iniciados.
4. Abrir SQL*Plus y comprobar la conexión de la siguiente sección.

Si Oracle está en una VM, sustituir `localhost` por la dirección accesible de esa VM en SQL*Plus y en la URL JDBC. Mantener el servidor dentro de la red de desarrollo.

## 3. Crear el usuario Oracle del proyecto

En la terminal, iniciar SQL*Plus sin escribir contraseñas en el comando:

```powershell
sqlplus /nolog
```

Dentro de SQL*Plus:

```sql
CONNECT SYSTEM@//localhost:1521/XE
```

SQL*Plus pide la contraseña administrativa. Crear el esquema una sola vez:

```sql
SET ECHO OFF
SET VERIFY OFF
ACCEPT clave CHAR PROMPT 'Clave nueva de COOPERATIVA: ' HIDE
```

Escribir la contraseña solicitada y presionar Enter. Después ejecutar:

```sql
CREATE USER COOPERATIVA IDENTIFIED BY "&clave"
  DEFAULT TABLESPACE USERS
  TEMPORARY TABLESPACE TEMP
  QUOTA 100M ON USERS;
UNDEFINE clave
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO COOPERATIVA;
CONNECT COOPERATIVA@//localhost:1521/XE
SHOW USER
```

Para este bloque de sustitución, escoger una contraseña ASCII de hasta 30 caracteres, sin `"` ni `&`. `ACCEPT ... HIDE` la solicita sin mostrarla; `SET VERIFY OFF` evita imprimir la sustitución. No usar `SPOOL` durante la creación. Véanse [ACCEPT de SQL*Plus 10g](https://docs.oracle.com/cd/B19306_01/server.102/b14357/ch12005.htm) y [CREATE USER de Oracle 10g](https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_8003.htm).

El usuario puede conectarse y crear sus propias tablas y secuencias; no recibe `DBA` ni permisos sobre otros esquemas. La cuota de 100 MB es para desarrollo y el administrador puede ampliarla si hace falta. Para los scripts posteriores, `SHOW USER` debe mostrar **COOPERATIVA**, no SYSTEM ni SYS.

## 4. Cargar y comprobar Oracle

Abrir SQL*Plus desde la raíz del proyecto. Si no está en `PATH`, usar el `sqlplus.exe` de la instalación XE. Tras conectarse como `COOPERATIVA`:

```sql
@scripts/01_schema_oracle.sql
@scripts/02_initial_data.sql
EXIT
```

Para verificar, abrir una sesión nueva con `sqlplus /nolog` y ejecutar:

```sql
CONNECT COOPERATIVA@//localhost:1521/XE
SHOW USER
@scripts/07_verify_oracle.sql
```

Leer la salida de cada archivo antes de continuar. Ante un error, detenerse y corregir su causa. Oracle confirma automáticamente el DDL; una creación interrumpida puede dejar parte de la estructura. Para rehacer una base local de prueba, usar la limpieza de la sección 8 y volver a ejecutar 01 y 02.

El verificador requiere los datos ficticios originales del script 02. Comprueba las 15 PK, las 19 FK y las 14 secuencias; intenta actualizar referencias a padres inexistentes y eliminar padres que tienen hijos. También intenta insertar PK duplicadas y comprueba valores únicos, `NOT NULL` y `CHECK`. Los 73 casos de rechazo se revierten y no consumen secuencias. Debe terminar con `VERIFICACION ORACLE OK` y sin errores imprevistos; un fallo de una prueba significa que no se ha validado la instalación.

Las FK impiden referencias huérfanas y el borrado de un padre relacionado. `CHECK` limita montos, stock, cantidades y el indicador `titular` a valores válidos. Las reglas que dependen de varias filas, como mantener al menos un titular, las aplican los controllers en una transacción; las FK por sí solas no garantizan esas reglas.

Consultas de diagnóstico sin modificar datos:

```sql
SELECT table_name FROM user_tables ORDER BY table_name;
SELECT constraint_name, constraint_type, status, validated
FROM user_constraints
WHERE constraint_type IN ('P', 'R', 'U', 'C')
ORDER BY table_name, constraint_type, constraint_name;
SELECT sequence_name FROM user_sequences ORDER BY sequence_name;
```

En una instalación nueva del proyecto, las restricciones deben estar `ENABLED` y `VALIDATED`. Guardar la salida del verificador como evidencia de la máquina y versión probadas; disponer de los archivos SQL no equivale a haberlos ejecutado en Oracle.

## 5. Preparar PostgreSQL

Instalar la misma versión estable de PostgreSQL que acuerde el equipo. La [página oficial para Windows](https://www.postgresql.org/download/windows/) enlaza el instalador de EDB. Seleccionar servidor y herramientas de línea de comandos, recordar la contraseña del administrador `postgres` y conservar el puerto `5432` si está libre. Las instrucciones no requieren StackBuilder ni drivers adicionales.

Desde la raíz del proyecto, abrir `psql`; si no está en `PATH`, usar su ruta de instalación:

```powershell
psql -h localhost -p 5432 -U postgres -d postgres -W
```

Crear un rol de aplicación sin privilegios administrativos y una base dedicada:

```sql
CREATE ROLE cooperativa LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
\password cooperativa
```

Escribir y confirmar la contraseña cuando se solicite. Continuar con:

```sql
CREATE DATABASE cooperativa_respaldo OWNER cooperativa ENCODING 'UTF8';
\connect cooperativa_respaldo
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO cooperativa;
\quit
```

`\password` solicita y confirma la clave sin dejarla como literal SQL en el historial. Los permisos de creación se limitan a la base del proyecto. Referencias: [CREATE ROLE](https://www.postgresql.org/docs/current/sql-createrole.html), [CREATE DATABASE](https://www.postgresql.org/docs/current/sql-createdatabase.html) y [psql](https://www.postgresql.org/docs/current/app-psql.html).

Para instalar una **base de prueba** PostgreSQL:

```powershell
psql -h localhost -p 5432 -U cooperativa -d cooperativa_respaldo -W -v ON_ERROR_STOP=1 -f scripts/04_schema_postgresql.sql
psql -h localhost -p 5432 -U cooperativa -d cooperativa_respaldo -W -v ON_ERROR_STOP=1 -f scripts/05_initial_data_postgresql.sql
psql -h localhost -p 5432 -U cooperativa -d cooperativa_respaldo -W -v ON_ERROR_STOP=1 -f scripts/08_verify_postgresql.sql
```

El verificador PostgreSQL también necesita los datos ficticios del script 05 y revierte sus 73 intentos inválidos. Debe terminar con `VERIFICACION POSTGRESQL OK`. Para preparar un **destino vacío de respaldo**, ejecutar solamente 04. Si ya se cargó 05, el primer respaldo reemplaza esos datos por los que lea de Oracle.

La traducción conserva los nombres: `NUMBER(10)` → `INTEGER`, dinero/cantidades decimales → `NUMERIC`, `NUMBER(1)` → `SMALLINT`, `VARCHAR2` → `VARCHAR` y `DATE` → `TIMESTAMP WITHOUT TIME ZONE`. Los identificadores respetan el máximo `2147483647` de `int` Java. PostgreSQL mantiene 14 secuencias `seq_*`; la tabla puente conserva su PK compuesta. Los DAO de la aplicación siguen usando Oracle: el módulo de migración copia por JDBC directamente.

## 6. Configurar Java y probar la conexión secundaria

En la raíz del repositorio:

```powershell
Copy-Item config.properties.example config.properties
```

Editar `config.properties` con los datos locales reales:

```properties
db.url=jdbc:oracle:thin:@localhost:1521:XE
db.user=COOPERATIVA
db.password=TU_CLAVE_ORACLE

pg.url=jdbc:postgresql://localhost:5432/cooperativa_respaldo
pg.user=cooperativa
pg.password=TU_CLAVE_POSTGRESQL
```

Los valores `TU_CLAVE_...` son marcadores: sustituirlos localmente. `config.properties` se ignora en Git. No subirlo, no pasar claves en URLs/comandos y no incluirlo en capturas. El formato Java `.properties` exige duplicar una barra inversa literal dentro de un valor (`\\`).

Compilar con Java 8 desde la misma carpeta:

```powershell
ant clean jar
java -cp "build/classes;lib/*" com.cooperativa.config.ConexionPostgres
```

La segunda orden carga `org.postgresql.Driver` desde `lib/postgresql-42.7.13.jar` y comprueba la conexión configurada. Solo una ejecución exitosa contra el servidor instalado prueba la conexión real. En NetBeans, compilar con **F11**; para ejecutar la clase de prueba, seleccionar `ConexionPostgres.java` y **Run File**. Mantener la carpeta de trabajo en la raíz para que se encuentre `config.properties`.

No ejecutar `Main` solo para comprobar una conexión: actualmente es un arnés de pruebas de negocio y puede insertar o modificar datos.

## 7. Respaldo lógico en caliente y migración

Con Oracle accesible, los datos del proyecto cargados y la estructura PostgreSQL creada con 04:

```powershell
java -cp "build/classes;lib/*" com.cooperativa.utils.BackupEnCaliente respaldar
```

Este comando **reemplaza las 15 tablas del destino PostgreSQL dedicado**. La aplicación puede seguir escribiendo en Oracle durante la lectura. Ejecutar una sola copia a la vez y no usar el destino como base operativa de otra aplicación.

El módulo inicia Oracle con `SET TRANSACTION READ ONLY` antes de consultar datos: las 15 tablas se leen desde la misma foto de datos ya confirmados. Es la garantía de consistencia transaccional descrita en [Oracle 10g SET TRANSACTION](https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_10005.htm). La copia inserta padres antes que hijos, conserva las PK y compara el número de filas de cada tabla en origen y destino. El conteo detecta pérdidas de filas; no constituye una comparación criptográfica de cada valor.

En PostgreSQL, el reemplazo se realiza en una transacción con bloqueo del destino. Se alinean sus secuencias con el siguiente ID libre mediante `ALTER SEQUENCE ... RESTART WITH`; esta operación es transaccional, a diferencia de `setval`, según [ALTER SEQUENCE](https://www.postgresql.org/docs/current/sql-altersequence.html). Ante un fallo anterior al `commit`, se revierte la copia y se conserva el respaldo anterior. Revisar los 15 conteos y la confirmación final `Respaldo consistente confirmado en PostgreSQL (15 tablas, 14 secuencias).`, además del código de salida del proceso: `0` éxito, `1` fallo de operación y `2` uso incorrecto de argumentos.

Si se pierde la conexión durante el `commit`, aparece `SQLState=08007`: el resultado de la confirmación es desconocido. Reconectar y revisar las 15 tablas antes de reintentar o declarar válido el respaldo; un error de comunicación en ese momento no demuestra que se haya revertido. `SYS` se rechaza como origen porque Oracle no garantiza para ese usuario la foto de solo lectura.

La primera ejecución sirve como migración completa; repetirla produce nuevas copias consistentes. No convierte los DAO Oracle en DAO PostgreSQL ni cambia automáticamente la base de la aplicación.

### Repetir automáticamente en Windows

Para una prueba académica, el equipo puede configurar el **Programador de tareas** tras completar una copia manual exitosa:

1. Crear una tarea con la cuenta local que puede leer `config.properties` y acceder a las dos bases.
2. Programa: ruta completa al `java.exe` del **JDK 8** del proyecto.
3. Argumentos: `-cp "build/classes;lib/*" com.cooperativa.utils.BackupEnCaliente respaldar`.
4. **Iniciar en**: ruta completa a la raíz del repositorio, sin comillas en ese campo.
5. Elegir la frecuencia acordada, por ejemplo cada 15 minutos; en **Settings**, seleccionar no iniciar una instancia nueva si ya hay otra activa.
6. Ejecutar la tarea manualmente una vez y revisar su resultado. Un cambio de ruta del proyecto requiere actualizarla.

Esta configuración es un paso local del operador; los scripts no crean una tarea programada por sí solos. No poner contraseñas de BD en sus argumentos.

### Alcance y pérdida de datos posible

El respaldo contiene el estado confirmado al comienzo de la lectura Oracle. Se pueden perder las operaciones posteriores a esa foto si Oracle cae. Con copias manuales, la antigüedad de la última copia correcta no tiene un límite fijo. Una frecuencia de 15 minutos es un objetivo operativo: fallos, demoras o un equipo apagado aumentan la pérdida posible (RPO). El tiempo de recuperación (RTO) depende de reinstalar Oracle, importar y validar los datos.

Se conserva una única copia lógica vigente en PostgreSQL. No hay failover automático, réplica continua ni recuperación hasta cada transacción. Este respaldo no incluye usuarios, privilegios, archivos físicos o configuración de Oracle; complementar con el respaldo físico administrado por el responsable de la instancia cuando corresponda. Si la copia larga falla con `ORA-01555`, conservar el último respaldo correcto y revisar con el administrador la retención de UNDO antes de reintentar.

### Conservar versiones fuera del repositorio

Después de una copia exitosa, guardar un archivo fechado en una carpeta de respaldos protegida, preferiblemente en otra máquina/disco:

```powershell
pg_dump -h localhost -p 5432 -U cooperativa -d cooperativa_respaldo -W --format=custom --no-owner --no-acl --file="D:\RespaldosCooperativa\cooperativa_2026-10-04_1800.dump"
```

Crear antes la carpeta y sustituir ruta/fecha por las reales; no se crea automáticamente. Usar las herramientas cliente de la versión del servidor PostgreSQL o una versión compatible posterior. `-W` solicita la contraseña. `pg_dump` produce un respaldo consistente mientras hay actividad concurrente; no incluye los roles globales del clúster. Véase [pg_dump](https://www.postgresql.org/docs/current/app-pgdump.html).

Para revisar una versión histórica, crear una base PostgreSQL vacía de laboratorio propiedad de `cooperativa`, por ejemplo `cooperativa_recuperacion`, y restaurar allí:

```powershell
pg_restore -h localhost -p 5432 -U cooperativa -d cooperativa_recuperacion -W --no-owner --no-acl --single-transaction "D:\RespaldosCooperativa\cooperativa_2026-10-04_1800.dump"
```

No ejecutar 04 antes: el archivo incluye estructura y datos. `--single-transaction` permite aplicar la restauración completa o revertirla ante un error; véase [pg_restore](https://www.postgresql.org/docs/current/app-pgrestore.html). Para recuperar Oracle desde esta versión, configurar temporalmente `pg.url` hacia `cooperativa_recuperacion` y seguir la sección 9. Los `.dump` y sus datos personales reales se conservan fuera de Git.

## 8. Reiniciar una base local de prueba

La limpieza elimina datos y estructura. Cerrar la aplicación y confirmar que la conexión apunta a la copia de desarrollo. En Oracle, conectado al esquema del proyecto:

```sql
SHOW USER
@scripts/03_drop_all.sql
```

En PostgreSQL:

```powershell
psql -h localhost -p 5432 -U cooperativa -d cooperativa_respaldo -W -v ON_ERROR_STOP=1 -f scripts/06_drop_all_postgresql.sql
```

Ambos scripts solicitan escribir `BORRAR_COOPERATIVA`. Eliminan las 15 tablas y 14 secuencias conocidas, hijos antes que padres, sin `CASCADE`. Una dependencia ajena debe provocar un error para que se investigue. No usar el respaldo vigente como base de pruebas de limpieza. Tras limpiar, volver a cargar los archivos en el orden de la tabla inicial.

## 9. Recuperar tras una caída de Oracle

1. Detener la aplicación y la tarea de respaldo. Conservar PostgreSQL y la versión `.dump` elegida. No sobrescribir un respaldo válido con una instalación Oracle recién creada y vacía.
2. Reparar o reinstalar Oracle XE en el entorno compatible. Crear un **esquema nuevo**, por ejemplo `COOPERATIVA_RECUPERADA`, con el mismo procedimiento de la sección 3 cambiando el nombre de usuario. Darle `CREATE SESSION`, `CREATE TABLE`, `CREATE SEQUENCE` y cuota en `USERS`.
3. Conectarse al nuevo usuario y ejecutar únicamente `@scripts/01_schema_oracle.sql`. No ejecutar 02: la recuperación requiere las 15 tablas vacías y las FK activas.
4. Añadir localmente estas claves a `config.properties`, manteniendo `db.*` como referencia de la instancia anterior y `pg.*` hacia el respaldo seleccionado:

   ```properties
   restore.oracle.url=jdbc:oracle:thin:@localhost:1521:XE
   restore.oracle.user=COOPERATIVA_RECUPERADA
   restore.oracle.password=TU_CLAVE_RECUPERACION
   ```

5. Crear una carpeta local fuera de Git para el ajuste de secuencias y ejecutar desde la raíz:

   ```powershell
   java -cp "build/classes;lib/*" com.cooperativa.utils.BackupEnCaliente recuperar "D:\RespaldosCooperativa\ajustar_secuencias_recuperadas.sql"
   ```

   La ruta de archivo debe ser nueva: el módulo no sobrescribe un archivo existente. Si se omite el argumento, usa `backups/recuperacion_secuencias.sql`, una carpeta ignorada por Git; crea las carpetas necesarias. Para repetir una recuperación de prueba, usar otro nombre de archivo.

   El módulo lee una foto consistente de PostgreSQL, conserva los IDs y fechas, carga padres antes que hijos y compara los conteos de las 15 tablas. Exige un usuario de recuperación diferente de `db.user` y tablas Oracle vacías. Si falla antes de confirmar, revierte las inserciones. Con `SQLState=08007`, la confirmación es incierta y se conserva el archivo de ajuste: reconectar y comparar las 15 tablas antes de continuar. Si los datos están completos, aplicar el ajuste; si el destino está vacío, reintentar con una ruta nueva. No ejecutar el ajuste sobre una recuperación cuyo resultado todavía no se ha verificado.

6. **Sin abrir todavía la aplicación**, conectarse por SQL*Plus a `COOPERATIVA_RECUPERADA` y ejecutar el archivo que produjo la recuperación:

   ```sql
   SHOW USER
   @"D:/RespaldosCooperativa/ajustar_secuencias_recuperadas.sql"
   ```

   Oracle 10g confirma DDL automáticamente. Por eso el ajuste de sus 14 secuencias se aplica después de la transacción de datos, con la aplicación detenida; deja el siguiente ID por encima del mayor restaurado. Si este ajuste falla, los datos ya confirmados siguen presentes: corregirlo antes de habilitar escrituras. No repetir la importación sobre tablas llenas.

7. Revisar los conteos registrados, las FK `ENABLED`/`VALIDATED` y una muestra de personas, cuentas, saldos, préstamos y albaranes. En una recuperación de los datos ficticios originales también se puede ejecutar 07; sus casos dependen de esos registros y no son una auditoría genérica de cualquier respaldo.
8. Cambiar `db.url`, `db.user` y `db.password` hacia el esquema recuperado, probar el flujo de negocio y habilitar la aplicación. Reanudar los respaldos solo después de validar esa nueva base. Guardar fecha de la foto recuperada y de la apertura del servicio para la evidencia académica.

La lectura PostgreSQL usa una transacción de solo lectura con aislamiento `REPEATABLE READ`, de forma que sus consultas mantienen la misma foto durante la recuperación. Referencia: [aislamiento de transacciones PostgreSQL](https://www.postgresql.org/docs/current/transaction-iso.html). Si el proceso de recuperación no llegó a confirmar, comprobar el esquema vacío antes de reintentar; si ya confirmó datos, continuar con su ajuste de secuencias.

## 10. Problemas frecuentes y evidencia de entrega

Los resultados ya ejecutados, sus límites y los comandos para repetir las pruebas Java
se encuentran en [VALIDACION_BD.md](VALIDACION_BD.md). La integración local con Oracle
10g XE y PostgreSQL está validada, incluido el respaldo con escrituras concurrentes y
la recuperación. Cada integrante configura sus credenciales y repite la instalación local.

| Síntoma | Revisión |
| --- | --- |
| `sqlplus` o `psql` no se reconoce | Usar el ejecutable de la instalación o añadir su carpeta `bin` al PATH local. |
| Oracle `ORA-12541` | Servicio listener, host, puerto 1521 y red de la VM. |
| Oracle `ORA-01017` | Usuario/clave local y conexión al servicio XE correcto. |
| Oracle `ORA-01950` | Cuota del usuario en `USERS`; revisar con el administrador. |
| Oracle `ORA-02289` | El mismo usuario debe crear secuencias y ejecutar los DAO. |
| Oracle `ORA-02291` / `ORA-02292` | Falta un padre o se intenta borrar uno relacionado; las FK están protegiendo los datos. |
| PostgreSQL rechaza conexión | Servicio, puerto 5432, base/rol, clave y reglas de acceso del servidor. |
| `ClassNotFoundException` | Ejecutar en la raíz y comprobar los dos JAR en `lib/` y el classpath. |
| No encuentra `config.properties` | Carpeta de trabajo raíz; crear la copia local de la plantilla. |
| PK duplicada después de recuperar | Completar el ajuste de secuencias antes de permitir nuevas altas. |
| `SQLState=55P03` | Otra copia/recuperación está activa o no se pudo obtener el bloqueo; esperar a que termine y revisar. |
| `SQLState=25000` | Usuario Oracle inválido, conexiones con transacción previa o destino de recuperación con datos; usar el esquema nuevo vacío. |
| `SQLState=22003` | Identificadores fuera del rango Java o sin espacio para futuras altas; revisar los datos con el administrador. |
| `SQLState=08007` | Confirmación incierta por fallo de comunicación; reconectar y verificar el contenido antes de reintentar. |

Para la entrega, registrar: versión exacta de ambas BD y JDK; salida de 07 y 08 en la instalación demo; resultado de la conexión JDBC; conteos de las 15 tablas durante respaldo y recuperación; prueba de que un dato inválido se rechaza; y una prueba de recuperación en el esquema nuevo. Ocultar credenciales y usar datos ficticios en capturas. Los resultados deben corresponder a ejecuciones reales, no a una lectura del código.
