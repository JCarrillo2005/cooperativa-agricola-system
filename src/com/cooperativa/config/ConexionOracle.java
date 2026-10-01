package com.cooperativa.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionOracle {
    
    // Cambia 'XE' por tu SID de Oracle 10g si es distinto (ejemplo: ORCL)
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE"; 
    private static final String USER = "system"; // Tu usuario de Oracle
    private static final String PASSWORD = "julio123"; // Tu contraseña de Oracle
    
    private static Connection conexion = null;

    public static Connection getConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Conexión exitosa a Oracle Database 10g");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: No se encontró el driver ojdbc14.jar");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión a Oracle 10g: " + e.getMessage());
        }
        return conexion;
    }

    public static void cerrarConexion() {
        if (conexion != null) {
            try {
                conexion.close();
                System.out.println("🔒 Conexión cerrada.");
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexión: " + e.getMessage());
            }
        }
    }
}