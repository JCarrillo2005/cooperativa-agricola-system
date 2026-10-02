package com.cooperativa.main;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.dao.AlbaranDAO;
import com.cooperativa.dao.DetalleAlbaranDAO;
import com.cooperativa.dao.PersonaDAO;
import com.cooperativa.dao.ProductoDAO;
import com.cooperativa.model.Albaran;
import com.cooperativa.model.DetalleAlbaran;
import com.cooperativa.model.Persona;
import com.cooperativa.model.Producto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (!ConexionOracle.probarConexion()) {
            System.out.println("Falló la conexión. Revisa config.properties y que Oracle esté activo.");
            return;
        }
        System.out.println("Conexión exitosa a Oracle");

        try {
            probarAlbaranes();
        } catch (SQLException e) {
            System.err.println("Error SQL: " + e.getMessage());
        }
    }

    private static void probarAlbaranes() throws SQLException {
        List<Persona> personas = new PersonaDAO().obtenerTodos();
        List<Producto> productos = new ProductoDAO().obtenerTodos();
        if (personas.isEmpty() || productos.isEmpty()) {
            System.out.println("Faltan personas o productos para la prueba.");
            return;
        }
        Persona socio = personas.get(0);
        AlbaranDAO albaranDAO = new AlbaranDAO();

        // 1) Entrega válida
        String talonario = "TAL-" + (System.currentTimeMillis() % 100000000L);
        Albaran a = nuevoAlbaran(talonario, socio, productos);
        registrarAlbaran(a);
        System.out.println("Albarán " + a.getId() + " registrado, talonario " + talonario);

        Albaran guardado = albaranDAO.obtenerPorIdConDetalles(a.getId());
        BigDecimal rechazado = BigDecimal.ZERO;
        for (DetalleAlbaran d : guardado.getDetalles()) {
            System.out.println("  Producto " + d.getProductoId()
                + " | aceptado " + d.getCantidadAceptada()
                + " | rechazado " + d.getCantidadRechazada()
                + " | precio " + d.getPrecioUnitario()
                + " | subtotal " + d.getSubtotal().setScale(2, RoundingMode.HALF_UP));
            rechazado = rechazado.add(d.getCantidadRechazada());
        }
        System.out.println("Total a pagar al socio: "
            + guardado.calcularTotal().setScale(2, RoundingMode.HALF_UP));
        System.out.println("Total rechazado (kg): " + rechazado);

        // 2) Mismo talonario otra vez: debe rechazarse y no dejar nada
        int antes = albaranDAO.obtenerPorPersona(socio.getId()).size();
        try {
            registrarAlbaran(nuevoAlbaran(talonario, socio, productos));
        } catch (SQLException e) {
            System.out.println("Rechazado: " + e.getMessage());
        }
        int despues = albaranDAO.obtenerPorPersona(socio.getId()).size();
        System.out.println("Albaranes de " + socio + " antes: " + antes
            + " | después: " + despues + " (deben ser iguales)");
    }

    private static Albaran nuevoAlbaran(String talonario, Persona socio, List<Producto> productos) {
        Albaran a = new Albaran();
        a.setNumeroTalonario(talonario);
        a.setPersonaId(socio.getId());
        a.setSedeId(socio.getSedeId());

        // Línea 1: 80.50 kg aceptados, 4.25 kg rechazados, a 3.75 por kg
        a.agregarDetalle(new DetalleAlbaran(productos.get(0).getId(),
            new BigDecimal("80.50"), new BigDecimal("4.25"), new BigDecimal("3.75")));

        // Línea 2 (si hay un segundo producto): 40.00 kg aceptados, 0 rechazados, a 2.10 por kg
        if (productos.size() > 1) {
            a.agregarDetalle(new DetalleAlbaran(productos.get(1).getId(),
                new BigDecimal("40.00"), BigDecimal.ZERO, new BigDecimal("2.10")));
        }
        return a;
    }

    /** Transacción: encabezado del albarán + todas sus líneas, o nada. */
    private static void registrarAlbaran(Albaran a) throws SQLException {
        AlbaranDAO albaranDAO = new AlbaranDAO();
        DetalleAlbaranDAO detalleDAO = new DetalleAlbaranDAO();

        try (Connection conn = ConexionOracle.getConexion()) {
            conn.setAutoCommit(false);
            try {
                albaranDAO.guardar(a, conn);
                for (DetalleAlbaran d : a.getDetalles()) {
                    d.setAlbaranId(a.getId());
                    detalleDAO.guardar(d, conn);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}