package com.cooperativa.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Fábrica de conexiones JDBC hacia Oracle 10g con un pool sencillo.
 *
 * - Las credenciales se leen de config.properties (raíz del proyecto, ignorado por Git).
 * - getConexion() entrega una conexión del pool; al hacer close() NO se cierra de verdad:
 *   se deshace lo que no se confirmó y vuelve al pool para reutilizarse.
 * - Se mantienen como máximo MAXIMO_CONEXIONES conexiones físicas abiertas.
 */
public class ConexionOracle {

    private static final String ARCHIVO_CONFIG = "config.properties";
    private static final String DRIVER = "oracle.jdbc.driver.OracleDriver"; // ojdbc14.jar

    private static final int MAXIMO_CONEXIONES = 10;
    private static final long ESPERA_MAXIMA_MS = 15000;

    private static final Object LOCK = new Object();
    private static final LinkedList<Connection> libres = new LinkedList<Connection>();
    private static int creadas = 0;   // conexiones físicas existentes (libres + en uso)

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

    // ---------- API pública ----------

    /**
     * Entrega una conexión del pool. Quien la pida debe cerrarla (try-with-resources):
     *
     * try (Connection conn = ConexionOracle.getConexion()) { ... }
     */
    public static Connection getConexion() throws SQLException {
        return envolver(tomar());
    }

    /** Utilidad rápida para verificar que la conexión funciona. */
    public static boolean probarConexion() {
        try (Connection conn = getConexion()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Error de conexion: " + e.getMessage());
            return false;
        }
    }

    /** Cierra de verdad las conexiones libres del pool. Llamar al cerrar la aplicación. */
    public static void cerrarTodas() {
        synchronized (LOCK) {
            while (!libres.isEmpty()) {
                cerrarSilencioso(libres.removeFirst());
                creadas--;
            }
        }
    }

    // ---------- Pool ----------

    private static Connection tomar() throws SQLException {
        synchronized (LOCK) {
            long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
            while (true) {
                // 1) Reutilizar una conexión libre
                while (!libres.isEmpty()) {
                    Connection c = libres.removeFirst();
                    if (!c.isClosed()) {
                        return c;
                    }
                    creadas--;
                }
                // 2) Crear una nueva si no se llegó al máximo
                if (creadas < MAXIMO_CONEXIONES) {
                    creadas++;
                    try {
                        return DriverManager.getConnection(url, usuario, password);
                    } catch (SQLException e) {
                        creadas--;
                        throw e;
                    }
                }
                // 3) Esperar a que alguien devuelva una
                long restante = limite - System.currentTimeMillis();
                if (restante <= 0) {
                    throw new SQLException("No hay conexiones disponibles (todas estan en uso).");
                }
                try {
                    LOCK.wait(restante);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Se interrumpio la espera de una conexion.");
                }
            }
        }
    }

    private static void devolver(Connection fisica) {
        boolean reutilizable = false;
        try {
            if (!fisica.isClosed()) {
                if (!fisica.getAutoCommit()) {
                    fisica.rollback();            // descarta lo que no se confirmó
                    fisica.setAutoCommit(true);   // la próxima persona la recibe limpia
                }
                reutilizable = true;
            }
        } catch (SQLException e) {
            reutilizable = false;                 // si algo falla, se descarta
        }
        synchronized (LOCK) {
            if (reutilizable) {
                libres.addLast(fisica);
            } else {
                cerrarSilencioso(fisica);
                creadas--;
            }
            LOCK.notifyAll();
        }
    }

    private static void cerrarSilencioso(Connection c) {
        try {
            c.close();
        } catch (SQLException e) {
            // se ignora
        }
    }

    /** Envuelve la conexión física para que close() la devuelva al pool. */
    private static Connection envolver(final Connection fisica) {
        InvocationHandler manejador = new InvocationHandler() {
            private boolean devuelta = false;

            @Override
            public Object invoke(Object proxy, Method metodo, Object[] args) throws Throwable {
                String nombre = metodo.getName();

                if (nombre.equals("close")) {
                    if (!devuelta) {
                        devuelta = true;
                        devolver(fisica);
                    }
                    return null;
                }
                if (nombre.equals("isClosed")) {
                    return devuelta || fisica.isClosed();
                }
                if (devuelta) {
                    throw new SQLException("La conexion ya fue cerrada.");
                }
                try {
                    return metodo.invoke(fisica, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        };
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, manejador);
    }
}