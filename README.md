# Sistema de Gestión Integral - Cooperativa Agrícola

[![Java](https://img.shields.io/badge/Java-8%20%7C%2011-orange.svg)](https://www.oracle.com/java/)
[![Oracle](https://img.shields.io/badge/Oracle_DB-10g-red.svg)](https://www.oracle.com/database/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-OpenSource-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/Academic-UMG-green.svg)]()

Sistema informático desarrollado para la automatización de procesos operacionales, financieros y de acopio de una **Cooperativa Agrícola**. Permite el control de socios y empleados, manejo de cuentas de ahorro y saldos, otorgamiento de créditos e insumos agrícolas (semillas, abonos, insecticidas), recepción de hortalizas con evaluación de control de calidad y la emisión automatizada de albaranes.

---

## Integrantes del Equipo

| Nombre                          | Rol Principal                                                 |
| :------------------------------ | :------------------------------------------------------------ |
| **Julio César Carrillo García** | **Coordinador & Desarrollador Backend JAVA**                  |
| **José Adrián**                 | **Modelado & DB Admin (Oracle / PostgreSQL)**                 |
| **Jorge Leonidas**              | **Desarrollador Frontend JAVA (Swing/GUI)**                   |
| **Nahomi López**                | **Documentadora Líder & Pruebas**                             |
| **Marvin Muñoz**                | **Documentador & Diseñador (Resumen Ejecutivo / Infografía)** |

---

## Arquitectura del Sistema (Patrón MVC / DAO)

El proyecto está estructurado modularmente para garantizar la separación de responsabilidades y el código limpio:

```text
SistemaCooperativaAgricola/
 ├── src/
 │    └── com/cooperativa/
 │         ├── config/       # Clases de conexión JDBC (Oracle 10g y PostgreSQL)
 │         ├── dao/          # Data Access Object (Lógica CRUD y transacciones SQL)
 │         ├── model/        # Entidades POJO (Persona, Cuenta, Prestamo, Producto, etc.)
 │         ├── utils/        # Generador de reportes/albaranes en PDF y módulo Hot Backup
 │         ├── view/         # Interfaces gráficas de usuario (JFrames / Swing)
 │         └── main/         # Clase principal de ejecución
 ├── lib/                    # Driver JDBC (ojdbc14.jar / postgresql.jar)
 ├── scripts/                # Scripts DDL/DML para Oracle 10g y PostgreSQL
 └── README.md
```

## Tecnologías y Recursos Utilizados

- Lenguaje: Java (JDK 8 / JDK 11)
- Entorno de Desarrollo (IDE): Apache NetBeans 31
- Base de Datos Principal: Oracle Database 10g (Release 10.2.0.1.0)
- Base de Datos Secundaria / Migración: PostgreSQL
- Drivers JDBC:
- ojdbc14.jar (Oracle Database)
- postgresql-42.x.x.jar (PostgreSQL)

- Control de Versiones: Git y GitHub

## Requisitos de Instalación y Ejecución

Prerrequisitos
JDK 8 instalado y configurado en las variables de entorno (JAVA_HOME).

Instancia funcional de Oracle Database 10g escuchando en el puerto 1521 (SID: XE u ORCL).

Instancia de PostgreSQL para la ejecución de pruebas de migración y backups en caliente (Fase IV).
