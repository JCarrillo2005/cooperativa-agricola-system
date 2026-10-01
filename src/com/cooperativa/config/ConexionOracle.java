package com.cooperativa.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fabrica de conexiones JDBC hacia Oracle 10g.
 *
 * - Las credenciales NO viven en el codigo: se leen de config.properties
 *   (en la raiz del proyecto, ignorado por Git).
 * - Cada llamada a getConexion() devuelve una conexion NUEVA. Asi cada DAO
 *   puede cerrarla con try-with-resources y las transacciones
 *   (setAutoCommit(false), commit, rollback) no se mezclan entre si.
 */
public class ConexionOracle {

    private static final String ARCHIVO_CONFIG = "config.properties";
    private static final String DRIVER = "oracle.jdbc.driver.OracleDriver"; // ojdbc14.jar

    private static String url;
    private static String usuario;
    private static String password;

    static {
        try {
            cargarConfiguracion();
            Class.forName(DRIVER);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                "No se pudo leer " + ARCHIVO_CONFIG + ". Copia config.properties.example "
                + "como config.properties en la raiz del proyecto y ajusta tus datos. ("
                + e.getMessage() + ")");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                "No se encontro el driver de Oracle. Revisa que lib/ojdbc14.jar este en el classpath.");
        }
    }

    private ConexionOracle() {
        // Clase utilitaria: no se instancia
    }

    private static void cargarConfiguracion() throws IOException {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(ARCHIVO_CONFIG)) {
            props.load(in);
        }
        url = props.getProperty("db.url");
        usuario = props.getProperty("db.user");
        password = props.getProperty("db.password");

        if (url == null || usuario == null || password == null) {
            throw new IOException("Faltan db.url, db.user o db.password en " + ARCHIVO_CONFIG);
        }
    }

    /**
     * Abre una conexion nueva. Quien la pida es responsable de cerrarla:
     *
     * try (Connection conn = ConexionOracle.getConexion()) { ... }
     */
    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(url, usuario, password);
    }

    /** Utilidad rapida para verificar que la conexion funciona. */
    public static boolean probarConexion() {
        try (Connection conn = getConexion()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Error de conexion: " + e.getMessage());
            return false;
        }
    }
}
