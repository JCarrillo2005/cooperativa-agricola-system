package com.cooperativa.main;

import com.cooperativa.config.ConexionOracle;

public class Main {
    public static void main(String[] args) {
        // Prueba de conexión inicial
        if (ConexionOracle.probarConexion()) {
            System.out.println("Conexión exitosa a Oracle");
        } else {
            System.out.println("Falló la conexión. Revisa config.properties y que Oracle esté activo.");
        }
    }
}