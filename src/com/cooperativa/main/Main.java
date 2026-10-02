package com.cooperativa.main;

import com.cooperativa.config.ConexionOracle;
import com.cooperativa.controller.AlbaranController;
import com.cooperativa.controller.Catalogos;
import com.cooperativa.controller.CuentaController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.PersonaController;
import com.cooperativa.controller.PrestamoController;
import com.cooperativa.dao.ProductoDAO;
import com.cooperativa.dao.SedeDAO;
import com.cooperativa.dao.TipoCuentaDAO;
import com.cooperativa.model.Albaran;
import com.cooperativa.model.Cuenta;
import com.cooperativa.model.CuentaPersona;
import com.cooperativa.model.DetalleAlbaran;
import com.cooperativa.model.DetallePrestamo;
import com.cooperativa.model.Persona;
import com.cooperativa.model.Prestamo;
import com.cooperativa.model.Producto;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (!ConexionOracle.probarConexion()) {
            System.out.println("Falló la conexión. Revisa config.properties y que Oracle esté activo.");
            return;
        }
        System.out.println("Conexión exitosa a Oracle");

        try {
            Persona socio = probarPersonas();
            probarCuentas(socio);
            probarPrestamos(socio);
            probarAlbaranes(socio);
        } catch (NegocioException e) {
            System.out.println("Error de negocio inesperado: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Error SQL: " + e.getMessage());
        }
    }

    // ---------- Personas ----------

    private static Persona probarPersonas() throws NegocioException, SQLException {
        System.out.println("\n=== PERSONAS ===");
        PersonaController pc = new PersonaController();

        Persona p = new Persona();
        p.setNombre("Marta");
        p.setApellido("López");
        p.setDni("T" + (System.currentTimeMillis() % 1000000000L));
        p.setDireccion("Aldea El Roble");
        p.setTelefono("55550000");
        p.setTipoPersonaId(Catalogos.tipoPersonaId(Catalogos.TIPO_PERSONA_SOCIO));
        p.setSedeId(new SedeDAO().obtenerTodos().get(0).getId());

        pc.registrar(p);   // sin estatus: queda ACTIVO
        System.out.println("Persona registrada con id " + p.getId() + " (" + p + ")");

        try {
            pc.registrar(p);   // mismo DNI
        } catch (NegocioException e) {
            System.out.println("Rechazado: " + e.getMessage());
        }

        try {
            pc.registrar(new Persona());   // sin datos
        } catch (NegocioException e) {
            System.out.println("Rechazado: " + e.getMessage());
        }
        return p;
    }

    // ---------- Cuentas ----------

    private static void probarCuentas(Persona socio) throws NegocioException, SQLException {
        System.out.println("\n=== CUENTAS ===");
        CuentaController cc = new CuentaController();

        Cuenta cuenta = new Cuenta();
        cuenta.setTipoCuentaId(new TipoCuentaDAO().obtenerTodos().get(0).getId());
        cuenta.setSedeId(socio.getSedeId());

        List<CuentaPersona> personas = new ArrayList<CuentaPersona>();
        personas.add(new CuentaPersona(0, socio.getId(), true));

        cc.abrirCuenta(cuenta, personas, new BigDecimal("200.00"));
        System.out.println("Cuenta " + cuenta.getNumero() + " abierta con saldo "
            + cc.consultarSaldo(cuenta.getId()));

        System.out.println("Saldo tras depositar 50.25: " + cc.depositar(cuenta.getId(), new BigDecimal("50.25")));

        try {
            cc.retirar(cuenta.getId(), new BigDecimal("1000"));
        } catch (NegocioException e) {
            System.out.println("Rechazado: " + e.getMessage());
        }

        System.out.println("Saldo tras retirar 30.00: " + cc.retirar(cuenta.getId(), new BigDecimal("30.00")));

        // Una cuenta sin titular no se puede abrir
        List<CuentaPersona> sinTitular = new ArrayList<CuentaPersona>();
        sinTitular.add(new CuentaPersona(0, socio.getId(), false));
        try {
            cc.abrirCuenta(new Cuenta(), sinTitular, null);
        } catch (NegocioException e) {
            System.out.println("Rechazado: " + e.getMessage());
        }
    }

    // ---------- Préstamos ----------

    private static void probarPrestamos(Persona socio) throws NegocioException, SQLException {
        System.out.println("\n=== PRÉSTAMOS ===");
        PrestamoController prc = new PrestamoController();
        PersonaController pc = new PersonaController();
        Producto producto = new ProductoDAO().obtenerTodos().get(0);
        int tipoConsumo = Catalogos.tipoPrestamoId(Catalogos.TIPO_PRESTAMO_CONSUMO);

        Prestamo p = nuevoPrestamoConsumo(socio, tipoConsumo, producto, 3);
        prc.registrarPrestamo(p);
        System.out.println("Préstamo " + p.getId() + " registrado por " + p.getMonto());

        try {
            prc.registrarPrestamo(nuevoPrestamoConsumo(socio, tipoConsumo, producto, 1000000));
        } catch (NegocioException e) {
            System.out.println("Rechazado: " + e.getMessage());
        }

        // Persona inactiva: no puede recibir préstamos
        pc.desactivar(socio.getId());
        try {
            prc.registrarPrestamo(nuevoPrestamoConsumo(socio, tipoConsumo, producto, 1));
        } catch (NegocioException e) {
            System.out.println("Rechazado: " + e.getMessage());
        }
        pc.activar(socio.getId());
    }

    private static Prestamo nuevoPrestamoConsumo(Persona socio, int tipoId, Producto producto, int cantidad) {
        Prestamo p = new Prestamo();
        p.setPersonaId(socio.getId());
        p.setTipoPrestamoId(tipoId);
        p.setTasaInteres(new BigDecimal("5.00"));

        Calendar vence = Calendar.getInstance();
        vence.add(Calendar.DAY_OF_YEAR, 180);
        p.setFechaVencimiento(vence.getTime());

        p.agregarDetalle(new DetallePrestamo(producto.getId(), cantidad, producto.getPrecio()));
        return p;
    }

    // ---------- Albaranes ----------

    private static void probarAlbaranes(Persona socio) throws NegocioException, SQLException {
        System.out.println("\n=== ALBARANES ===");
        AlbaranController ac = new AlbaranController();
        List<Producto> productos = new ProductoDAO().obtenerTodos();

        String talonario = "TAL-" + (System.currentTimeMillis() % 100000000L);
        Albaran a = nuevoAlbaran(talonario, socio, productos);
        ac.registrarEntrega(a);
        System.out.println("Albarán " + a.getId() + " registrado. Total a pagar: " + ac.totalAPagar(a));

        try {
            ac.registrarEntrega(nuevoAlbaran(talonario, socio, productos));   // mismo talonario
        } catch (NegocioException e) {
            System.out.println("Rechazado: " + e.getMessage());
        }
    }

    private static Albaran nuevoAlbaran(String talonario, Persona socio, List<Producto> productos) {
        Albaran a = new Albaran();
        a.setNumeroTalonario(talonario);
        a.setPersonaId(socio.getId());   // la sede se toma del socio

        a.agregarDetalle(new DetalleAlbaran(productos.get(0).getId(),
            new BigDecimal("80.50"), new BigDecimal("4.25"), new BigDecimal("3.75")));
        return a;
    }
}