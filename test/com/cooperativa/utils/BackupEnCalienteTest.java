package com.cooperativa.utils;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pruebas de fallos JDBC sin conexiones reales ni dependencias adicionales. */
public final class BackupEnCalienteTest {
    private BackupEnCalienteTest() { }

    public static void main(String[] argumentos) throws Exception {
        respaldoConservaTiposYOrden();
        respaldoRevierteFalloEnLote();
        respaldoRevierteFalloTardioEnSecuencia();
        respaldoRechazaConcurrente();
        respaldoRechazaSys();
        recuperacionExigeVacio();
        recuperacionNoEjecutaDdl();
        recuperacionNoSobrescribeArchivo();
        recuperacionConservaSqlSiCommitEsIncierto();
        System.out.println("9 pruebas de respaldo/recuperación OK (dobles JDBC; no sustituyen pruebas contra los motores).");
    }

    private static void respaldoConservaTiposYOrden() throws Exception {
        Motor origen = new Motor(27, false);
        Motor destino = new Motor(3, false);
        Map<String, Long> conteos = BackupEnCaliente.respaldar(origen.conexion(), destino.conexion());
        exigir(conteos.size() == 15 && destino.confirmaciones == 1, "Debe confirmar las 15 tablas.");
        exigir(origen.sql.get(0).equals("SET TRANSACTION READ ONLY"), "La primera sentencia debe fijar la foto Oracle.");
        exigir(Arrays.equals(origen.filas.get("productos").get(0), destino.filas.get("productos").get(0)),
                "Debe conservar texto, enteros y BigDecimal.");
        exigir(Arrays.equals(origen.filas.get("prestamos").get(0), destino.filas.get("prestamos").get(0)),
                "Debe conservar fechas con hora y null.");
        List<String> borradas = new ArrayList<String>();
        List<String> insertadas = new ArrayList<String>();
        for (String sentencia : destino.sql) {
            if (sentencia.startsWith("DELETE FROM ")) { borradas.add(sentencia.substring(12)); }
            if (sentencia.startsWith("INSERT INTO ")) { insertadas.add(sentencia.split(" ")[2]); }
        }
        exigir(borradas.get(0).equals("detalles_albaran") && borradas.get(14).equals("sedes"),
                "El borrado debe respetar las FKs desde hijos a padres.");
        exigir(insertadas.get(0).equals("sedes") && insertadas.get(14).equals("detalles_albaran"),
                "La inserción debe empezar por los padres.");
        exigir(destino.secuencias.size() == 14 && destino.secuencias.get("seq_personas") == 28L,
                "El próximo id debe superar los importados.");
    }

    private static void respaldoRevierteFalloEnLote() throws Exception {
        Motor origen = new Motor(27, false);
        Motor destino = new Motor(3, false);
        destino.fallarLote = "productos";
        esperarSQL("23514", new Accion() { public void ejecutar() throws Exception {
            BackupEnCaliente.respaldar(origen.conexion(), destino.conexion());
        }});
        exigir(destino.confirmaciones == 0 && destino.reversiones > 0, "No confirmar una copia incompleta.");
        exigir(((Number) destino.filas.get("sedes").get(0)[0]).intValue() == 3,
                "El respaldo anterior debe sobrevivir al fallo de un lote después del borrado.");
    }

    private static void respaldoRevierteFalloTardioEnSecuencia() throws Exception {
        Motor origen = new Motor(27, false);
        Motor destino = new Motor(3, false);
        destino.fallarSecuencia = "seq_productos";
        esperarSQL("42501", new Accion() { public void ejecutar() throws Exception {
            BackupEnCaliente.respaldar(origen.conexion(), destino.conexion());
        }});
        exigir(destino.secuencias.get("seq_sedes") == 4L, "RESTART debe deshacerse junto con los datos.");
        exigir(((Number) destino.filas.get("personas").get(0)[0]).intValue() == 3,
                "Un fallo tardío conserva toda la copia anterior.");
    }

    private static void respaldoRechazaConcurrente() throws Exception {
        Motor origen = new Motor(27, false);
        Motor destino = new Motor(3, false);
        destino.bloqueoDisponible = false;
        esperarSQL("55P03", new Accion() { public void ejecutar() throws Exception {
            BackupEnCaliente.respaldar(origen.conexion(), destino.conexion());
        }});
        for (String sentencia : destino.sql) {
            exigir(!sentencia.startsWith("DELETE"), "No borrar antes de adquirir el bloqueo exclusivo.");
        }
    }

    private static void respaldoRechazaSys() throws Exception {
        Motor origen = new Motor(27, false);
        origen.usuario = "SYS";
        Motor destino = new Motor(3, false);
        esperarSQL("25000", new Accion() { public void ejecutar() throws Exception {
            BackupEnCaliente.respaldar(origen.conexion(), destino.conexion());
        }});
        exigir(destino.sql.isEmpty(), "SYS no debe reemplazar el respaldo existente.");
    }

    private static void recuperacionExigeVacio() throws Exception {
        Motor postgres = new Motor(27, false);
        Motor oracle = new Motor(3, false);
        Path carpeta = Files.createTempDirectory("cooperativa-recuperacion-test-");
        Path archivo = carpeta.resolve("ajuste.sql");
        try {
            esperarSQL("25000", new Accion() { public void ejecutar() throws Exception {
                BackupEnCaliente.recuperar(postgres.conexion(), oracle.conexion(), archivo);
            }});
            exigir(!Files.exists(archivo), "No crear SQL si el destino contiene datos.");
            exigir(oracle.confirmaciones == 0, "No confirmar en un destino no vacío.");
        } finally { Files.deleteIfExists(archivo); Files.delete(carpeta); }
    }

    private static void recuperacionNoEjecutaDdl() throws Exception {
        Motor postgres = new Motor(27, false);
        Motor oracle = new Motor(1, true);
        Path carpeta = Files.createTempDirectory("cooperativa-recuperacion-test-");
        Path archivo = carpeta.resolve("ajuste.sql");
        try {
            Map<String, Long> conteos = BackupEnCaliente.recuperar(postgres.conexion(), oracle.conexion(), archivo);
            exigir(conteos.size() == 15 && oracle.confirmaciones == 1, "Recuperar y comprobar todas las tablas.");
            exigir(postgres.aislamiento == Connection.TRANSACTION_REPEATABLE_READ && postgres.soloLectura,
                    "La fuente PostgreSQL debe proporcionar una sola foto de lectura.");
            for (String sentencia : oracle.sql) {
                exigir(!sentencia.startsWith("ALTER") && !sentencia.startsWith("DELETE"),
                        "No ejecutar DDL ni borrar datos durante la recuperación.");
            }
            String ajuste = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
            exigir(ajuste.contains("ALTER SEQUENCE seq_personas") && !ajuste.contains("DROP SEQUENCE"),
                    "El ajuste conserva las secuencias y se ejecuta por separado.");
            exigir(ajuste.contains("v_maximo - v_actual") && !ajuste.contains("MAX(id), 0) + 1"),
                    "No consumir el último id disponible al ajustar una secuencia Oracle.");
        } finally { Files.deleteIfExists(archivo); Files.delete(carpeta); }
    }

    private static void recuperacionNoSobrescribeArchivo() throws Exception {
        Motor postgres = new Motor(27, false);
        Motor oracle = new Motor(1, true);
        Path carpeta = Files.createTempDirectory("cooperativa-recuperacion-test-");
        Path archivo = carpeta.resolve("ajuste.sql");
        Files.write(archivo, "conservar".getBytes(StandardCharsets.UTF_8));
        try {
            try {
                BackupEnCaliente.recuperar(postgres.conexion(), oracle.conexion(), archivo);
                throw new AssertionError("Debe rechazar un archivo existente.");
            } catch (IOException esperada) { }
            exigir(oracle.filas.get("personas").isEmpty() && oracle.confirmaciones == 0,
                    "Revertir datos si no se pudo crear el SQL.");
            exigir(new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8).equals("conservar"),
                    "No sobrescribir ni eliminar un archivo ajeno.");
        } finally { Files.delete(archivo); Files.delete(carpeta); }
    }

    private static void recuperacionConservaSqlSiCommitEsIncierto() throws Exception {
        Motor postgres = new Motor(27, false);
        Motor oracle = new Motor(1, true);
        oracle.fallarDespuesCommit = true;
        Path carpeta = Files.createTempDirectory("cooperativa-recuperacion-test-");
        Path archivo = carpeta.resolve("ajuste.sql");
        try {
            esperarSQL("08007", new Accion() { public void ejecutar() throws Exception {
                BackupEnCaliente.recuperar(postgres.conexion(), oracle.conexion(), archivo);
            }});
            exigir(Files.exists(archivo), "Conservar el ajuste si el servidor pudo confirmar el commit.");
            exigir(!oracle.filas.get("personas").isEmpty(), "El doble simula commit aplicado seguido de caída de red.");
        } finally { Files.deleteIfExists(archivo); Files.delete(carpeta); }
    }

    private interface Accion { void ejecutar() throws Exception; }
    private static void esperarSQL(String estado, Accion accion) throws Exception {
        try { accion.ejecutar(); throw new AssertionError("Se esperaba SQLState " + estado); }
        catch (SQLException ex) { exigir(estado.equals(ex.getSQLState()), "SQLState incorrecto: " + ex.getSQLState()); }
    }
    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) { throw new AssertionError(mensaje); }
    }

    /** Simula estado transaccional para observar rollback ante fallos a mitad de copia. */
    private static final class Motor implements InvocationHandler {
        Map<String, List<Object[]>> filas = new LinkedHashMap<String, List<Object[]>>();
        Map<String, Long> secuencias = new LinkedHashMap<String, Long>();
        Map<String, List<Object[]>> anteriores;
        Map<String, Long> secuenciasAnteriores;
        List<String> sql = new ArrayList<String>();
        boolean autoCommit = true;
        boolean soloLectura;
        boolean bloqueoDisponible = true;
        boolean fallarDespuesCommit;
        String fallarLote;
        String fallarSecuencia;
        String usuario = "COOPERATIVA_QA";
        int confirmaciones;
        int reversiones;
        int aislamiento;

        Motor(int id, boolean vacio) {
            for (EsquemaRespaldo.Tabla tabla : EsquemaRespaldo.TABLAS) {
                List<Object[]> registros = new ArrayList<Object[]>();
                if (!vacio) {
                    Object[] fila = new Object[tabla.tipos.length];
                    for (int indice = 0; indice < fila.length; indice++) {
                        switch (tabla.tipos[indice]) {
                            case Types.INTEGER: fila[indice] = id; break;
                            case Types.SMALLINT: fila[indice] = 1; break;
                            case Types.NUMERIC: fila[indice] = new BigDecimal("123.45"); break;
                            case Types.TIMESTAMP: fila[indice] = Timestamp.valueOf("2026-10-01 12:34:56"); break;
                            default: fila[indice] = "Texto con ñ";
                        }
                    }
                    if (tabla.nombre.equals("prestamos")) { fila[7] = null; }
                    registros.add(fila);
                }
                filas.put(tabla.nombre, registros);
                if (tabla.secuencia != null) { secuencias.put(tabla.secuencia, (long) id + 1); }
            }
        }

        Connection conexion() { return proxy(Connection.class, this); }

        @Override public Object invoke(Object objeto, Method metodo, Object[] argumentos) throws Throwable {
            String nombre = metodo.getName();
            if (nombre.equals("getAutoCommit")) { return autoCommit; }
            if (nombre.equals("setAutoCommit")) {
                autoCommit = (Boolean) argumentos[0];
                if (!autoCommit) {
                    anteriores = copiar(filas);
                    secuenciasAnteriores = new LinkedHashMap<String, Long>(secuencias);
                }
                return null;
            }
            if (nombre.equals("setReadOnly")) { soloLectura = (Boolean) argumentos[0]; return null; }
            if (nombre.equals("setTransactionIsolation")) { aislamiento = (Integer) argumentos[0]; return null; }
            if (nombre.equals("commit")) {
                anteriores = null; secuenciasAnteriores = null; confirmaciones++;
                if (fallarDespuesCommit) { throw new SQLException("Fallo simulado de red", "08006"); }
                return null;
            }
            if (nombre.equals("rollback")) {
                if (anteriores != null) { filas = copiar(anteriores); secuencias = new LinkedHashMap<String, Long>(secuenciasAnteriores); }
                reversiones++; return null;
            }
            if (nombre.equals("prepareStatement")) { return sentencia((String) argumentos[0]); }
            if (nombre.equals("close")) { return null; }
            throw new UnsupportedOperationException("Método JDBC no esperado: " + nombre);
        }

        PreparedStatement sentencia(final String consulta) {
            return proxy(PreparedStatement.class, new InvocationHandler() {
                Object[] valores = new Object[32];
                List<Object[]> lote = new ArrayList<Object[]>();
                @Override public Object invoke(Object objeto, Method metodo, Object[] argumentos) throws Throwable {
                    String nombre = metodo.getName();
                    if (nombre.equals("setFetchSize") || nombre.equals("close")) { return null; }
                    if (nombre.startsWith("set")) {
                        valores[(Integer) argumentos[0] - 1] = nombre.equals("setNull") ? null : argumentos[1];
                        return null;
                    }
                    if (nombre.equals("addBatch")) { lote.add(valores.clone()); return null; }
                    if (nombre.equals("clearBatch")) { lote.clear(); return null; }
                    if (nombre.equals("executeBatch")) {
                        sql.add(consulta);
                        String tabla = consulta.split(" ")[2];
                        if (tabla.equals(fallarLote)) { throw new SQLException("Fallo simulado de lote", "23514"); }
                        int columnas = 0;
                        for (EsquemaRespaldo.Tabla especificacion : EsquemaRespaldo.TABLAS) {
                            if (especificacion.nombre.equals(tabla)) { columnas = especificacion.tipos.length; }
                        }
                        for (Object[] fila : lote) { filas.get(tabla).add(Arrays.copyOf(fila, columnas)); }
                        int[] cantidades = new int[lote.size()]; Arrays.fill(cantidades, 1); return cantidades;
                    }
                    if (nombre.equals("executeQuery")) {
                        sql.add(consulta);
                        if (consulta.contains("pg_try_advisory")) { return resultado(Arrays.asList(new Object[][] {{bloqueoDisponible}})); }
                        if (consulta.equals("SELECT USER FROM dual")) { return resultado(Arrays.asList(new Object[][] {{usuario}})); }
                        String tabla = consulta.split(" FROM ")[1].split(" ")[0];
                        if (consulta.startsWith("SELECT COUNT")) { return resultado(Arrays.asList(new Object[][] {{(long) filas.get(tabla).size()}})); }
                        if (consulta.contains("MAX(id)")) {
                            long maximo = 0;
                            for (Object[] fila : filas.get(tabla)) { maximo = Math.max(maximo, ((Number) fila[0]).longValue()); }
                            return resultado(Arrays.asList(new Object[][] {{maximo}}));
                        }
                        return resultado(filas.get(tabla));
                    }
                    if (nombre.equals("execute")) {
                        sql.add(consulta);
                        if (consulta.startsWith("DELETE FROM ")) { filas.get(consulta.substring(12)).clear(); }
                        if (consulta.startsWith("ALTER SEQUENCE ")) {
                            String[] partes = consulta.split(" ");
                            if (partes[2].equals(fallarSecuencia)) { throw new SQLException("Fallo simulado de permiso", "42501"); }
                            secuencias.put(partes[2], Long.valueOf(partes[5]));
                        }
                        return false;
                    }
                    throw new UnsupportedOperationException("Método sentencia no esperado: " + nombre);
                }
            });
        }
    }

    private static ResultSet resultado(final List<Object[]> filas) {
        return proxy(ResultSet.class, new InvocationHandler() {
            int indice = -1;
            boolean nulo;
            @Override public Object invoke(Object objeto, Method metodo, Object[] argumentos) {
                String nombre = metodo.getName();
                if (nombre.equals("next")) { return ++indice < filas.size(); }
                if (nombre.equals("close")) { return null; }
                if (nombre.equals("wasNull")) { return nulo; }
                Object valor = filas.get(indice)[(Integer) argumentos[0] - 1];
                nulo = valor == null;
                if (nombre.equals("getLong")) { return valor == null ? 0L : ((Number) valor).longValue(); }
                if (nombre.equals("getBoolean")) { return valor; }
                if (nombre.equals("getString") || nombre.equals("getBigDecimal") || nombre.equals("getTimestamp")) { return valor; }
                throw new UnsupportedOperationException("Método resultado no esperado: " + nombre);
            }
        });
    }

    private static Map<String, List<Object[]>> copiar(Map<String, List<Object[]>> origen) {
        Map<String, List<Object[]>> destino = new LinkedHashMap<String, List<Object[]>>();
        for (Map.Entry<String, List<Object[]>> tabla : origen.entrySet()) {
            List<Object[]> filas = new ArrayList<Object[]>();
            for (Object[] fila : tabla.getValue()) { filas.add(fila.clone()); }
            destino.put(tabla.getKey(), filas);
        }
        return destino;
    }

    private static <T> T proxy(Class<T> tipo, InvocationHandler manejador) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] {tipo}, manejador));
    }
}
