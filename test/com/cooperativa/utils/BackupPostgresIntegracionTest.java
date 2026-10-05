package com.cooperativa.utils;

import com.cooperativa.config.ConexionPostgres;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Integración con DOS PostgreSQL exclusivos de QA, con nombres terminados en _qa.
 * Origen con scripts 04+05; destino con solo 04. MODIFICA las dos bases QA.
 * Usa PostgreSQL como fuente sustituta y adapta SELECT USER FROM dual: no prueba Oracle.
 * Comprueba JDBC real, valores copiados, rollback de datos/secuencias y bloqueo concurrente.
 */
public final class BackupPostgresIntegracionTest {
    private BackupPostgresIntegracionTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || args[0].equals(args[1])) {
            throw new IllegalArgumentException("Indicar dos URLs PostgreSQL de QA diferentes.");
        }
        for (String url : args) {
            if (!url.startsWith("jdbc:postgresql://") || !url.endsWith("_qa")) {
                throw new IllegalArgumentException("Solo se permiten bases PostgreSQL terminadas en _qa.");
            }
        }
        Properties base = ConexionPostgres.cargarConfiguracion();
        Properties origen = configuracion(base, args[0]);
        Properties destino = configuracion(base, args[1]);
        Map<String, Long> conteos = copiar(origen, destino, false);
        comprobar(conteos.size() == 15, "Deben copiarse las 15 tablas.");
        comprobar(filas(origen).equals(filas(destino)), "Valores, fechas, nulos o decimales diferentes.");
        List<String> copiaAnterior = filas(destino);
        // Avanzar una secuencia para que rollback de RESTART sea observable.
        try (Connection c = ConexionPostgres.abrir(destino)) {
            ejecutar(c, "SELECT nextval('seq_productos')");
        }
        List<String> secuenciasAnteriores = secuencias(destino);
        try (Connection c = ConexionPostgres.abrir(origen)) {
            ejecutar(c, "UPDATE productos SET precio = precio + 0.10 WHERE id = (SELECT MIN(id) FROM productos)");
        }
        try {
            copiar(origen, destino, true);
            throw new AssertionError("El fallo tardío debía impedir el commit.");
        } catch (SQLException esperado) {
            comprobar("XX999".equals(esperado.getSQLState()), "Falló por una causa distinta a la inyectada.");
        }
        comprobar(copiaAnterior.equals(filas(destino)), "El fallo destruyó la copia anterior.");
        comprobar(secuenciasAnteriores.equals(secuencias(destino)), "RESTART no se revirtió.");

        try (Connection bloqueo = ConexionPostgres.abrir(destino)) {
            bloqueo.setAutoCommit(false);
            ejecutar(bloqueo, "SELECT pg_advisory_xact_lock(20261004, 15)");
            try {
                copiar(origen, destino, false);
                throw new AssertionError("Debía rechazar el respaldo concurrente.");
            } catch (SQLException esperado) {
                comprobar("55P03".equals(esperado.getSQLState()), "No detectó el bloqueo concurrente.");
            } finally {
                bloqueo.rollback();
            }
        }
        comprobar(copiaAnterior.equals(filas(destino)), "La concurrencia modificó la copia anterior.");
        copiar(origen, destino, false);
        comprobar(filas(origen).equals(filas(destino)), "El segundo respaldo no reemplazó todos los valores.");
        try (Connection c = ConexionPostgres.abrir(destino)) {
            for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
                if (tabla.secuencia == null) { continue; }
                try (PreparedStatement p = c.prepareStatement("SELECT nextval('" + tabla.secuencia
                        + "') > COALESCE(MAX(id), 0) FROM " + tabla.nombre);
                     ResultSet r = p.executeQuery()) {
                    comprobar(r.next() && r.getBoolean(1), "Secuencia desfasada: " + tabla.nombre);
                }
            }
        }
        System.out.println("INTEGRACION POSTGRESQL OK: 15 tablas, valores, rollback de datos/14 secuencias, concurrencia y reemplazo.");
        System.out.println("Fuente PostgreSQL sustituta: no acredita conexión, snapshot ni recuperación Oracle reales.");
    }

    private static Properties configuracion(Properties base, String url) {
        Properties p = new Properties();
        p.putAll(base);
        p.setProperty("pg.url", url);
        return p;
    }

    private static Map<String, Long> copiar(Properties origen, Properties destino, boolean fallar) throws SQLException {
        try (Connection o = ConexionPostgres.abrir(origen); Connection d = ConexionPostgres.abrir(destino)) {
            return BackupEnCaliente.respaldar(adaptar(o, false), adaptar(d, fallar));
        }
    }

    private static Connection adaptar(final Connection real, final boolean fallar) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class }, (proxy, metodo, args) -> {
                    if ("prepareStatement".equals(metodo.getName()) && args != null && args[0] instanceof String) {
                        String sql = (String) args[0];
                        if ("SELECT USER FROM dual".equals(sql)) {
                            args[0] = "SELECT current_user";
                        }
                        if (fallar && sql.startsWith("ALTER SEQUENCE seq_detalles_albaran RESTART")) {
                            throw new SQLException("Fallo tardío de prueba.", "XX999");
                        }
                    }
                    try { return metodo.invoke(real, args); }
                    catch (InvocationTargetException ex) { throw ex.getCause(); }
                });
    }

    private static List<String> filas(Properties p) throws SQLException {
        List<String> filas = new ArrayList<String>();
        try (Connection c = ConexionPostgres.abrir(p)) {
            for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
                try (PreparedStatement s = c.prepareStatement(tabla.seleccionar()); ResultSet r = s.executeQuery()) {
                    while (r.next()) {
                        for (int i = 1; i <= tabla.tipos.length; i++) {
                            filas.add(tabla.nombre + ":" + i + ":" + r.getString(i));
                        }
                    }
                }
            }
        }
        return filas;
    }

    private static List<String> secuencias(Properties p) throws SQLException {
        List<String> valores = new ArrayList<String>();
        try (Connection c = ConexionPostgres.abrir(p)) {
            for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
                if (tabla.secuencia == null) { continue; }
                try (PreparedStatement s = c.prepareStatement("SELECT last_value, is_called FROM " + tabla.secuencia);
                     ResultSet r = s.executeQuery()) {
                    comprobar(r.next(), "Secuencia ausente.");
                    valores.add(tabla.secuencia + ":" + r.getLong(1) + ":" + r.getBoolean(2));
                }
            }
        }
        return valores;
    }

    private static void ejecutar(Connection c, String sql) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { p.execute(); }
    }

    private static void comprobar(boolean valido, String mensaje) {
        if (!valido) { throw new AssertionError(mensaje); }
    }
}
