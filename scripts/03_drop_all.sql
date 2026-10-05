-- DESTRUCTIVO: elimina exclusivamente las 15 tablas y 14 secuencias de esta app.
-- Sin CASCADE: si otro objeto depende de ellas, se detiene para no borrarlo.
-- Hacer respaldo antes. Las tablas Oracle se borran con PURGE (sin papelera).
SET DEFINE ON
SET VERIFY OFF
WHENEVER SQLERROR EXIT FAILURE ROLLBACK
WHENEVER OSERROR EXIT FAILURE ROLLBACK
ACCEPT confirmar CHAR PROMPT 'Escriba BORRAR_COOPERATIVA para borrar la app: '
DECLARE
  PROCEDURE quitar(sentencia VARCHAR2, ausente NUMBER) IS
  BEGIN
    EXECUTE IMMEDIATE sentencia;
  EXCEPTION WHEN OTHERS THEN
    IF SQLCODE <> ausente THEN RAISE; END IF;
  END;
BEGIN
  IF NVL('&confirmar', 'SIN_CONFIRMACION') <> 'BORRAR_COOPERATIVA' THEN
    RAISE_APPLICATION_ERROR(-20020, 'Cancelado: confirmacion incorrecta.');
  END IF;
  quitar('DROP TABLE detalles_albaran PURGE', -942);
  quitar('DROP TABLE albaranes PURGE', -942);
  quitar('DROP TABLE detalles_prestamo PURGE', -942);
  quitar('DROP TABLE prestamos PURGE', -942);
  quitar('DROP TABLE saldos PURGE', -942);
  quitar('DROP TABLE cuentas_personas PURGE', -942);
  quitar('DROP TABLE cuentas PURGE', -942);
  quitar('DROP TABLE productos PURGE', -942);
  quitar('DROP TABLE personas PURGE', -942);
  quitar('DROP TABLE categorias_producto PURGE', -942);
  quitar('DROP TABLE tipos_prestamo PURGE', -942);
  quitar('DROP TABLE tipos_cuenta PURGE', -942);
  quitar('DROP TABLE tipos_persona PURGE', -942);
  quitar('DROP TABLE estatus PURGE', -942);
  quitar('DROP TABLE sedes PURGE', -942);
  quitar('DROP SEQUENCE seq_detalles_albaran', -2289);
  quitar('DROP SEQUENCE seq_albaranes', -2289);
  quitar('DROP SEQUENCE seq_detalles_prestamo', -2289);
  quitar('DROP SEQUENCE seq_prestamos', -2289);
  quitar('DROP SEQUENCE seq_saldos', -2289);
  quitar('DROP SEQUENCE seq_cuentas', -2289);
  quitar('DROP SEQUENCE seq_productos', -2289);
  quitar('DROP SEQUENCE seq_personas', -2289);
  quitar('DROP SEQUENCE seq_categorias_producto', -2289);
  quitar('DROP SEQUENCE seq_tipos_prestamo', -2289);
  quitar('DROP SEQUENCE seq_tipos_cuenta', -2289);
  quitar('DROP SEQUENCE seq_tipos_persona', -2289);
  quitar('DROP SEQUENCE seq_estatus', -2289);
  quitar('DROP SEQUENCE seq_sedes', -2289);
END;
/
UNDEFINE confirmar
SET DEFINE OFF
PROMPT Objetos de la cooperativa eliminados.
