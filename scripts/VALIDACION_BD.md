# Validación del aporte de base de datos

Fecha: 2026-10-04. Base de código: `main` en `38850703dd0d1acbceb9bac77a2c94d8eec174c6`.
Pruebas realizadas en una copia local, antes de commit y publicación.

## Resultados ejecutados

- Compilación de **todo `src/`** con Eclipse Temurin **Java 8u504**, `javac -source 1.8 -target 1.8`, UTF-8 y los dos JAR existentes en `lib/`: correcta.
- PostgreSQL **16.15**, instancia temporal exclusiva en `127.0.0.1:55434`, sin instalar un servicio de Windows: DDL 04 y datos 05 ejecutados correctamente, con **15 tablas, 14 secuencias y 38 filas ficticias**.
- `08_verify_postgresql.sql`: **73 rechazos esperados** (19 FK, 19 borrados de padres relacionados, 15 duplicados y 20 casos adicionales). Finalizó con rollback y `VERIFICACION POSTGRESQL OK`.
- `ConexionPostgres`: conexión real por JDBC usando **`postgresql-42.7.13.jar`** del repositorio: correcta.
- `BackupEnCalienteTest`: **9 pruebas con dobles JDBC** aprobadas: orden de copia, tipos/nulos/fechas, fallo de lote, fallo de secuencia, concurrencia, rechazo de SYS, destino de recuperación no vacío, archivo SQL y commit incierto. Son pruebas de comportamiento, no una ejecución de Oracle.
- `BackupPostgresIntegracionTest`: destino PostgreSQL real y fuente PostgreSQL sustituta. Copia de las **15 tablas** con comparación de valores; fallo tardío conserva los datos anteriores y revierte los cambios en las **14 secuencias**; bloqueo concurrente rechazado; copia repetida correcta y próximos IDs por encima de los importados.
- `06_drop_all_postgresql.sql`: una confirmación incorrecta conservó las 15 tablas; `BORRAR_COOPERATIVA` eliminó los objetos en la base de QA.
- `git diff --check`: sin errores de espacios. Ninguna credencial o archivo de respaldo forma parte del aporte.

## Validación real de Oracle y recuperación

Después de obtener acceso local autorizado se ejecutaron las pruebas en **Oracle Database 10g XE 10.2.0.1.0**, usando dos usuarios nuevos exclusivos de QA, con cuota y privilegios de creación de sus propios objetos. Las tablas y datos preexistentes se conservaron.

- **01 + 02 + 07 en Oracle:** creación correcta de 15 tablas/14 secuencias, 38 filas ficticias y **73 rechazos de integridad aprobados**. Cada verificación acabó con rollback.
- **Copia real Oracle → PostgreSQL → Oracle:** las 15 tablas conservaron todas las columnas. Se verificaron importes decimales, `NULL`, fechas con hora y texto con `ñ`/tildes, usando `ojdbc14.jar` y `postgresql-42.7.13.jar` existentes.
- **Escritura concurrente real:** una segunda conexión Oracle confirmó una nueva sede y una persona relacionada entre la copia de sedes y personas. El respaldo mantuvo las 38 filas de su foto inicial, excluyendo ambas altas; la siguiente copia recogió las 40 filas confirmadas. No mezcló estados entre tablas.
- **Recuperación real:** se cargaron las 40 filas en un segundo esquema Oracle vacío y se compararon todos los valores con el origen y PostgreSQL.
- **Secuencias recuperadas:** SQL generado aplicado por SQL*Plus, 14 incrementos restaurados a 1, todos los `NEXTVAL` mayores que el ID máximo y una nueva alta válida (revertida después). El verificador 07 también aprobó las restricciones de la copia recuperada.
- **03 en Oracle:** confirmación incorrecta y entrada vacía conservaron las 15 tablas; confirmación correcta dejó cero tablas y secuencias en ambos esquemas QA.
- Al terminar, se retiraron los dos usuarios temporales vacíos, se apagó el PostgreSQL temporal y se eliminaron las credenciales de esta prueba. No se instalaron servicios ni se modificó la configuración global de Oracle.

La integración local queda validada. Para completar la prueba funcional en las máquinas del equipo:

1. Crear el usuario propio y ejecutar 01, 02 y 07 en Oracle XE 10g, según `README_BD.md`.
2. Configurar el usuario propio en Java y recorrer las pantallas/operaciones de negocio del equipo. La prueba concurrente ejecutada usó JDBC directamente; no certifica las pantallas Swing.
3. Ejecutar `recuperar` en un esquema Oracle nuevo y vacío, aplicar el SQL de secuencias generado y comprobar una nueva alta sin PK duplicada.

## Repetir las pruebas Java

Desde la raíz y con Java 8/Ant configurados:

```powershell
ant compile
New-Item -ItemType Directory -Path build/test-classes -Force | Out-Null
javac -encoding UTF-8 -source 1.8 -target 1.8 -cp "build/classes;lib/*" -d build/test-classes test/com/cooperativa/utils/BackupEnCalienteTest.java test/com/cooperativa/utils/BackupPostgresIntegracionTest.java
java -cp "build/classes;build/test-classes;lib/*" com.cooperativa.utils.BackupEnCalienteTest
```

Para la integración PostgreSQL, crear **dos bases exclusivas de laboratorio** con nombres terminados en `_qa`, con el mismo usuario/contraseña. En el origen cargar 04+05; en el destino cargar solamente 04. El `config.properties` local debe contener `pg.user` y `pg.password` válidos para ambas. La prueba modifica el precio de un producto en origen y reemplaza todas las tablas destino; no usar bases operativas ni respaldos reales.

```powershell
java -cp "build/classes;build/test-classes;lib/*" com.cooperativa.utils.BackupPostgresIntegracionTest jdbc:postgresql://localhost:5432/cooperativa_origen_qa jdbc:postgresql://localhost:5432/cooperativa_destino_qa
```

El adaptador de esta prueba convierte únicamente `SELECT USER FROM dual` en `SELECT current_user`. La fuente PostgreSQL admite `SET TRANSACTION READ ONLY`; así se verifica el destino transaccional real sin simular una conexión Oracle comprobada.

## Repetir la integración real de Oracle

`ValidacionOracleIntegracionTest.java` modifica solo un entorno de laboratorio preparado expresamente: origen Oracle con 01+02 y usuario `COOP_QA_*`, recuperación Oracle vacía con 01 y usuario `COOP_REC_*`, y destino PostgreSQL exclusivo terminado en `_qa` con solo 04. Configurar `db.*`, `pg.*` y `restore.oracle.*` localmente; el archivo de ajuste indicado debe ser nuevo. La prueba exige las 38 filas originales en origen, añade dos filas y reemplaza el destino PostgreSQL.

```powershell
javac -encoding UTF-8 -source 1.8 -target 1.8 -cp "build/classes;lib/*" -d build/test-classes test/com/cooperativa/utils/ValidacionOracleIntegracionTest.java
java -cp "build/classes;build/test-classes;lib/*" com.cooperativa.utils.ValidacionOracleIntegracionTest backups/ajuste_oracle_qa.sql
```

Después, ejecutar el ajuste generado conectado por SQL*Plus al usuario de recuperación, según la sección 9 de `README_BD.md`. No permitir altas hasta completar ese ajuste. No ejecutar este arnés sobre una instalación operativa ni sobre un respaldo vigente.
