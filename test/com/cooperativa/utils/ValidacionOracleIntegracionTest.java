package com.cooperativa.utils;

import com.cooperativa.config.ConexionPostgres;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prueba real Oracle -> PostgreSQL -> Oracle, exclusiva para bases de QA.
 * MODIFICA el origen (01+02), reemplaza el destino PG (solo 04) y llena la recuperación
 * Oracle (solo 01). Configurar usuarios COOP_QA_* y COOP_REC_* y una base PG terminada _qa.
 * Emite el ajuste de secuencias Oracle; se ejecuta después con SQL*Plus, no dentro del commit.
 */
public final class ValidacionOracleIntegracionTest {
    private ValidacionOracleIntegracionTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) { throw new IllegalArgumentException("Indicar ruta nueva de ajuste de secuencias."); }
        Properties config = ConexionPostgres.cargarConfiguracion();
        comprobar(config.getProperty("db.user", "").startsWith("COOP_QA_"), "El origen debe ser un usuario COOP_QA_*.");
        comprobar(config.getProperty("restore.oracle.user", "").startsWith("COOP_REC_"), "La recuperación debe ser COOP_REC_*.");
        comprobar(config.getProperty("pg.url", "").endsWith("_qa"), "La base PG debe terminar en _qa.");
        Class.forName("oracle.jdbc.driver.OracleDriver");
        try (Connection c = oracle(config, "db.")) {
            comprobar(filas(c).size() == 38, "Origen nuevo: requiere exactamente las 38 filas del script 02.");
            c.setAutoCommit(false);
            ejecutar(c, "UPDATE personas SET nombre = 'Ana ñ Á QA', fecha_registro = "
                    + "TO_DATE('2026-10-04 20:21:22','YYYY-MM-DD HH24:MI:SS') WHERE id = (SELECT MIN(id) FROM personas)");
            ejecutar(c, "UPDATE prestamos SET fecha_vencimiento = NULL WHERE id = (SELECT MIN(id) FROM prestamos)");
            ejecutar(c, "UPDATE productos SET precio = 19.97 WHERE id = (SELECT MIN(id) FROM productos)");
            c.commit();
        }
        List<String> fotoAnterior;
        try (Connection c = oracle(config, "db.")) { fotoAnterior = filas(c); }
        Map<String, Long> conteos = BackupEnCaliente.respaldar();
        comprobar(conteos.size() == 15, "Debe copiar las 15 tablas.");
        try (Connection pg = ConexionPostgres.abrir(config)) {
            comprobar(fotoAnterior.equals(filas(pg)), "La copia perdió valores, tildes, hora, nulos o decimales.");
        }

        // El escritor confirma un padre e hijo nuevos después de copiar sedes, antes de personas.
        // Una foto coherente debe excluir ambos aunque consultas posteriores ya ocurran tras commit.
        AtomicBoolean escritura = new AtomicBoolean(false);
        try (Connection lector = oracle(config, "db."); Connection pg = ConexionPostgres.abrir(config)) {
            Connection observado = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[] { Connection.class }, (proxy, metodo, argumentos) -> {
                        if ("prepareStatement".equals(metodo.getName()) && argumentos != null
                                && argumentos[0] instanceof String
                                && ((String) argumentos[0]).contains(" FROM personas ORDER BY ")
                                && escritura.compareAndSet(false, true)) {
                            try (Connection escritor = oracle(config, "db.")) {
                                escritor.setAutoCommit(false);
                                ejecutar(escritor, "INSERT INTO sedes (id, nombre) VALUES (900, 'SEDE CONCURRENTE QA')");
                                ejecutar(escritor, "INSERT INTO personas (id,nombre,apellido,dni,tipo_persona_id,estatus_id,sede_id) "
                                        + "VALUES (900,'Persona','Concurrente','QA-CONCURRENT-900',"
                                        + "(SELECT id FROM tipos_persona WHERE nombre='SOCIO'),"
                                        + "(SELECT id FROM estatus WHERE nombre='ACTIVO'),900)");
                                escritor.commit();
                            }
                        }
                        try { return metodo.invoke(lector, argumentos); }
                        catch (InvocationTargetException ex) { throw ex.getCause(); }
                    });
            BackupEnCaliente.respaldar(observado, pg);
        }
        comprobar(escritura.get(), "No se ejecutó el escritor concurrente.");
        try (Connection pg = ConexionPostgres.abrir(config); Connection nuevoOracle = oracle(config, "db.")) {
            comprobar(fotoAnterior.equals(filas(pg)), "El respaldo mezcló las dos fotos de Oracle.");
            comprobar(filas(nuevoOracle).size() == 40, "Oracle debe conservar el padre e hijo confirmados.");
        }
        // Copia siguiente debe recoger las dos altas nuevas y alinear sus secuencias destino.
        BackupEnCaliente.respaldar();
        try (Connection pg = ConexionPostgres.abrir(config); Connection fuente = oracle(config, "db.")) {
            comprobar(filas(fuente).equals(filas(pg)), "El respaldo siguiente no tomó los nuevos datos.");
        }
        Path ajuste = Paths.get(args[0]);
        Map<String, Long> recuperados = BackupEnCaliente.recuperar(ajuste);
        comprobar(recuperados.size() == 15, "Debe recuperar las 15 tablas.");
        try (Connection fuente = oracle(config, "db."); Connection pg = ConexionPostgres.abrir(config);
             Connection recuperacion = oracle(config, "restore.oracle.")) {
            comprobar(filas(fuente).equals(filas(pg)) && filas(pg).equals(filas(recuperacion)),
                    "El recorrido completo cambió valores.");
        }
        System.out.println("ORACLE INTEGRACION OK: 15 tablas, 40 filas, decimales, tildes, hora, nulos y foto con escritor concurrente.");
        System.out.println("Recuperación confirmada. Ejecutar el ajuste con SQL*Plus antes de probar nuevas altas: " + ajuste.toAbsolutePath());
    }

    private static Connection oracle(Properties p, String prefijo) throws SQLException {
        return DriverManager.getConnection(ConexionPostgres.requerida(p, prefijo + "url"),
                ConexionPostgres.requerida(p, prefijo + "user"), ConexionPostgres.requerida(p, prefijo + "password"));
    }

    private static List<String> filas(Connection c) throws SQLException {
        List<String> filas = new ArrayList<String>();
        for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
            try (PreparedStatement p = c.prepareStatement(tabla.seleccionar()); ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    StringBuilder fila = new StringBuilder(tabla.nombre);
                    for (int i = 0; i < tabla.tipos.length; i++) {
                        Object valor;
                        switch (tabla.tipos[i]) {
                            case Types.NUMERIC:
                                java.math.BigDecimal decimal = r.getBigDecimal(i + 1);
                                valor = decimal == null ? null : decimal.stripTrailingZeros().toPlainString();
                                break;
                            case Types.TIMESTAMP: valor = r.getTimestamp(i + 1); break;
                            case Types.INTEGER:
                            case Types.SMALLINT:
                                long entero = r.getLong(i + 1);
                                valor = r.wasNull() ? null : entero;
                                break;
                            default: valor = r.getString(i + 1);
                        }
                        String texto = valor == null ? null : valor.toString();
                        fila.append('|').append(texto == null ? -1 : texto.length()).append(':').append(texto);
                    }
                    filas.add(fila.toString());
                }
            }
        }
        return filas;
    }

    private static void ejecutar(Connection c, String sql) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { p.execute(); }
    }

    private static void comprobar(boolean valido, String mensaje) {
        if (!valido) { throw new AssertionError(mensaje); }
    }
}
