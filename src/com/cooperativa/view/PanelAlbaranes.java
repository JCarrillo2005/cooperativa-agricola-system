package com.cooperativa.view;

import com.cooperativa.controller.AlbaranController;
import com.cooperativa.controller.CatalogosController;
import com.cooperativa.controller.CuentaController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.PersonaController;
import com.cooperativa.controller.ProductoController;
import com.cooperativa.model.Albaran;
import com.cooperativa.model.Cuenta;
import com.cooperativa.model.DetalleAlbaran;
import com.cooperativa.model.Persona;
import com.cooperativa.model.Producto;
import com.cooperativa.model.Sede;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;

/**
 * Panel para recepción de cosechas agrícolas de los socios, control de calidad
 * (kilos aceptados vs rechazados), emisión de albaranes y liquidación con abono
 * a cuenta de ahorro.
 */
public class PanelAlbaranes extends JPanel {

    private final AlbaranController albaranController = new AlbaranController();
    private final PersonaController personaController = new PersonaController();
    private final ProductoController productoController = new ProductoController();
    private final CatalogosController catalogosController = new CatalogosController();
    private final CuentaController cuentaController = new CuentaController();

    // Controles de entrega
    private JComboBox<Persona> cmbSocio;
    private JComboBox<Sede> cmbSede;
    private JTextField txtNumeroTalonario;

    // Controles de control de calidad
    private JComboBox<Producto> cmbProducto;
    private JTextField txtCantidadAceptada;
    private JTextField txtCantidadRechazada;
    private JTextField txtPrecioUnitario;
    private JLabel lblTotalCalculado;

    private JButton btnAgregarLinea;
    private JButton btnQuitarLinea;
    private JTable tblLineasAlbaran;
    private DefaultTableModel modeloLineas;
    private final List<DetalleAlbaran> listaLineas = new ArrayList<DetalleAlbaran>();

    private JButton btnRegistrarAlbaran;
    private JButton btnLimpiar;

    // Tabla de albaranes registrados
    private JTable tblAlbaranes;
    private DefaultTableModel modeloAlbaranes;
    private JButton btnVerDesglose;
    private JButton btnLiquidarEnCuenta;
    private JButton btnRefrescar;

    public PanelAlbaranes() {
        initComponents();
        cargarCombos();
        cargarTablaAlbaranes();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel Superior: Formulario de Albarán y Evaluación de Calidad
        JPanel pnlNorte = new JPanel(new BorderLayout(10, 5));
        pnlNorte.setBorder(BorderFactory.createTitledBorder("Recepción de Cosechas y Emisión de Albarán"));

        // Datos del talonario
        JPanel pnlDatosTalonario = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        pnlDatosTalonario.add(new JLabel("Socio Agricultor:"), gbc);
        gbc.gridx = 1;
        cmbSocio = new JComboBox<Persona>();
        cmbSocio.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                actualizarComboSocios();
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        pnlDatosTalonario.add(cmbSocio, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        pnlDatosTalonario.add(new JLabel("Sede de Acopio:"), gbc);
        gbc.gridx = 1;
        cmbSede = new JComboBox<Sede>();
        pnlDatosTalonario.add(cmbSede, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        pnlDatosTalonario.add(new JLabel("No. de Talonario:"), gbc);
        gbc.gridx = 1;
        txtNumeroTalonario = new JTextField(14);
        pnlDatosTalonario.add(txtNumeroTalonario, gbc);

        pnlNorte.add(pnlDatosTalonario, BorderLayout.WEST);

        // Control de Calidad por Producto
        JPanel pnlCalidad = new JPanel(new BorderLayout(5, 5));
        pnlCalidad.setBorder(BorderFactory.createTitledBorder("Control de Calidad: Producto Aceptado vs. Rechazado"));

        JPanel pnlEntradaLinea = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        cmbProducto = new JComboBox<Producto>();
        cmbProducto.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                actualizarComboProductos();
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        cmbProducto.addActionListener(e -> sugerirPrecioProducto());
        
        txtCantidadAceptada = new JTextField("0.00", 5);
        txtCantidadRechazada = new JTextField("0.00", 5);
        txtPrecioUnitario = new JTextField("0.00", 5);

        btnAgregarLinea = new JButton("Agregar");
        btnQuitarLinea = new JButton("Quitar");
        btnAgregarLinea.addActionListener(e -> agregarLineaCalidad());
        btnQuitarLinea.addActionListener(e -> quitarLineaCalidad());

        pnlEntradaLinea.add(new JLabel("Producto:"));
        pnlEntradaLinea.add(cmbProducto);
        pnlEntradaLinea.add(new JLabel("Aceptado (kg):"));
        pnlEntradaLinea.add(txtCantidadAceptada);
        pnlEntradaLinea.add(new JLabel("Rechazado (kg):"));
        pnlEntradaLinea.add(txtCantidadRechazada);
        pnlEntradaLinea.add(new JLabel("Precio (Q):"));
        pnlEntradaLinea.add(txtPrecioUnitario);
        pnlEntradaLinea.add(btnAgregarLinea);
        pnlEntradaLinea.add(btnQuitarLinea);
        pnlCalidad.add(pnlEntradaLinea, BorderLayout.NORTH);

        modeloLineas = new DefaultTableModel(
            new Object[]{"ID", "Producto", "Aceptado", "Rechazado", "Precio Unitario", "Subtotal Liquidable"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLineasAlbaran = new JTable(modeloLineas);
        JScrollPane scrollLineas = new JScrollPane(tblLineasAlbaran);
        scrollLineas.setPreferredSize(new Dimension(500, 100));
        pnlCalidad.add(scrollLineas, BorderLayout.CENTER);

        pnlNorte.add(pnlCalidad, BorderLayout.CENTER);

        // Barra inferior del formulario: Total y botones
        JPanel pnlAccionNorte = new JPanel(new BorderLayout());
        JPanel pnlTotal = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel lblTexto = new JLabel("Total Aceptado a Pagar al Socio: ");
        lblTexto.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTotalCalculado = new JLabel("Q 0.00");
        lblTotalCalculado.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTotalCalculado.setForeground(new Color(0, 128, 0));
        pnlTotal.add(lblTexto);
        pnlTotal.add(lblTotalCalculado);

        JPanel pnlBotonesAlbaran = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnLimpiar = new JButton("Limpiar");
        btnRegistrarAlbaran = new JButton("Registrar Albarán");
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnRegistrarAlbaran.addActionListener(e -> registrarAlbaran());
        pnlBotonesAlbaran.add(btnLimpiar);
        pnlBotonesAlbaran.add(btnRegistrarAlbaran);

        pnlAccionNorte.add(pnlTotal, BorderLayout.WEST);
        pnlAccionNorte.add(pnlBotonesAlbaran, BorderLayout.EAST);
        pnlNorte.add(pnlAccionNorte, BorderLayout.SOUTH);

        add(pnlNorte, BorderLayout.NORTH);

        // Panel Central: Albaranes registrados y opciones de liquidación
        JPanel pnlCentro = new JPanel(new BorderLayout(5, 5));
        pnlCentro.setBorder(BorderFactory.createTitledBorder("Historial de Albaranes y Liquidación a Cuentas"));

        modeloAlbaranes = new DefaultTableModel(
            new Object[]{"ID", "No. Talonario", "Socio", "Fecha de Recepción", "Total Liquidable (Q)"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblAlbaranes = new JTable(modeloAlbaranes);
        tblAlbaranes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollAlbaranes = new JScrollPane(tblAlbaranes);
        scrollAlbaranes.setPreferredSize(new Dimension(800, 240));
        pnlCentro.add(scrollAlbaranes, BorderLayout.CENTER);

        JPanel pnlOperaciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnVerDesglose = new JButton("Ver Control de Calidad (Detalle)");
        btnLiquidarEnCuenta = new JButton("Abonar Pago a Cuenta de Ahorro del Socio");
        btnRefrescar = new JButton("Refrescar");

        btnVerDesglose.addActionListener(e -> verDesgloseCalidad());
        btnLiquidarEnCuenta.addActionListener(e -> liquidarEnCuentaSocio());
        btnRefrescar.addActionListener(e -> {
            cargarCombos();
            cargarTablaAlbaranes();
        });

        pnlOperaciones.add(btnVerDesglose);
        pnlOperaciones.add(btnLiquidarEnCuenta);
        pnlOperaciones.add(btnRefrescar);
        pnlCentro.add(pnlOperaciones, BorderLayout.SOUTH);

        add(pnlCentro, BorderLayout.CENTER);
    }

    private void cargarCombos() {
        try {
            actualizarComboSocios();

            List<Sede> sedes = catalogosController.listarSedes();
            cmbSede.setModel(new DefaultComboBoxModel<Sede>(sedes.toArray(new Sede[0])));

            actualizarComboProductos();
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error cargando datos: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarComboSocios() {
        try {
            Persona seleccionado = (Persona) cmbSocio.getSelectedItem();
            List<Persona> socios = personaController.listarSocios();
            cmbSocio.setModel(new DefaultComboBoxModel<Persona>(socios.toArray(new Persona[0])));
            if (seleccionado != null) {
                for (int i = 0; i < cmbSocio.getItemCount(); i++) {
                    if (cmbSocio.getItemAt(i).getId() == seleccionado.getId()) {
                        cmbSocio.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (NegocioException ignored) {}
    }

    private void actualizarComboProductos() {
        try {
            Producto seleccionado = (Producto) cmbProducto.getSelectedItem();
            List<Producto> productos = productoController.listarTodos();
            cmbProducto.setModel(new DefaultComboBoxModel<Producto>(productos.toArray(new Producto[0])));
            if (seleccionado != null) {
                for (int i = 0; i < cmbProducto.getItemCount(); i++) {
                    if (cmbProducto.getItemAt(i).getId() == seleccionado.getId()) {
                        cmbProducto.setSelectedIndex(i);
                        break;
                    }
                }
            }
            sugerirPrecioProducto();
        } catch (NegocioException ignored) {}
    }

    private void sugerirPrecioProducto() {
        Producto p = (Producto) cmbProducto.getSelectedItem();
        if (p != null) {
            txtPrecioUnitario.setText(p.getPrecio() != null ? p.getPrecio().toString() : "0.00");
        }
    }

    private void agregarLineaCalidad() {
        Producto p = (Producto) cmbProducto.getSelectedItem();
        if (p == null) return;

        BigDecimal aceptada, rechazada, precio;
        try {
            aceptada = new BigDecimal(txtCantidadAceptada.getText().trim());
            rechazada = new BigDecimal(txtCantidadRechazada.getText().trim());
            precio = new BigDecimal(txtPrecioUnitario.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ingresa valores numéricos válidos en cantidades y precio.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (aceptada.signum() < 0 || rechazada.signum() < 0) {
            JOptionPane.showMessageDialog(this, "Las cantidades no pueden ser negativas.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (aceptada.signum() == 0 && rechazada.signum() == 0) {
            JOptionPane.showMessageDialog(this, "Debe ingresar al menos una cantidad aceptada o rechazada.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DetalleAlbaran det = new DetalleAlbaran(p.getId(), aceptada, rechazada, precio);
        listaLineas.add(det);

        modeloLineas.addRow(new Object[]{
            p.getId(),
            p.getNombre(),
            aceptada,
            rechazada,
            "Q " + precio,
            "Q " + det.getSubtotal()
        });

        recalcularTotalAlbaran();
        txtCantidadAceptada.setText("0.00");
        txtCantidadRechazada.setText("0.00");
    }

    private void quitarLineaCalidad() {
        int fila = tblLineasAlbaran.getSelectedRow();
        if (fila >= 0) {
            listaLineas.remove(fila);
            modeloLineas.removeRow(fila);
            recalcularTotalAlbaran();
        } else {
            JOptionPane.showMessageDialog(this, "Selecciona una línea para quitar.", "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void recalcularTotalAlbaran() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleAlbaran d : listaLineas) {
            total = total.add(d.getSubtotal());
        }
        lblTotalCalculado.setText("Q " + total.setScale(2, RoundingMode.HALF_UP));
    }

    public void cargarTablaAlbaranes() {
        modeloAlbaranes.setRowCount(0);
        try {
            List<Albaran> lista = albaranController.listarTodos();
            for (Albaran a : lista) {
                Persona socio = personaController.obtenerPorId(a.getPersonaId());
                String nombreSocio = (socio != null) ? socio.getNombre() + " " + socio.getApellido() : String.valueOf(a.getPersonaId());
                BigDecimal total = albaranController.totalAPagar(albaranController.obtener(a.getId()));
                modeloAlbaranes.addRow(new Object[]{
                    a.getId(),
                    a.getNumeroTalonario(),
                    nombreSocio,
                    a.getFecha(),
                    "Q " + total
                });
            }
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error al listar albaranes: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrarAlbaran() {
        Persona socio = (Persona) cmbSocio.getSelectedItem();
        Sede sede = (Sede) cmbSede.getSelectedItem();
        String talonario = txtNumeroTalonario.getText().trim();

        if (socio == null || sede == null || talonario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completa socio, sede y número de talonario.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (listaLineas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El albarán debe contener al menos un producto evaluado.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Albaran a = new Albaran();
        a.setNumeroTalonario(talonario);
        a.setPersonaId(socio.getId());
        a.setSedeId(sede.getId());
        a.setDetalles(new ArrayList<DetalleAlbaran>(listaLineas));

        try {
            albaranController.registrarEntrega(a);
            BigDecimal total = albaranController.totalAPagar(a);
            JOptionPane.showMessageDialog(this,
                "Albarán registrado exitosamente.\nID: " + a.getId() + "\nTalonario: " + a.getNumeroTalonario()
                + "\nTotal Aceptado a Pagar: Q " + total,
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarTablaAlbaranes();
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso de Negocio", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void verDesgloseCalidad() {
        int fila = tblAlbaranes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un albarán de la tabla.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int albaranId = (Integer) modeloAlbaranes.getValueAt(fila, 0);
        try {
            Albaran a = albaranController.obtener(albaranId);
            StringBuilder sb = new StringBuilder("Detalle de Calidad - Talonario " + a.getNumeroTalonario() + ":\n\n");
            for (DetalleAlbaran d : a.getDetalles()) {
                Producto p = productoController.obtenerPorId(d.getProductoId());
                String nomProd = (p != null) ? p.getNombre() : "ID " + d.getProductoId();
                sb.append("• ").append(nomProd)
                  .append(" | Aceptado: ").append(d.getCantidadAceptada()).append(" kg")
                  .append(" | Rechazado: ").append(d.getCantidadRechazada()).append(" kg")
                  .append(" | Precio: Q ").append(d.getPrecioUnitario())
                  .append(" | Total: Q ").append(d.getSubtotal())
                  .append("\n");
            }
            sb.append("\nTOTAL ACREDITABLE (SOLO ACEPTADO): Q ").append(albaranController.totalAPagar(a));
            JOptionPane.showMessageDialog(this, sb.toString(), "Inspección de Calidad", JOptionPane.INFORMATION_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void liquidarEnCuentaSocio() {
        int fila = tblAlbaranes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona el albarán que deseas abonar.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int albaranId = (Integer) modeloAlbaranes.getValueAt(fila, 0);
        try {
            Albaran a = albaranController.obtener(albaranId);
            List<Cuenta> cuentas = cuentaController.cuentasDePersona(a.getPersonaId());
            if (cuentas.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "El socio no posee cuentas bancarias activas registradas en la cooperativa.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JComboBox<Cuenta> cmbCuentas = new JComboBox<Cuenta>(cuentas.toArray(new Cuenta[0]));
            int res = JOptionPane.showConfirmDialog(this, cmbCuentas,
                "Selecciona la cuenta de ahorro a acreditar:", JOptionPane.OK_CANCEL_OPTION);

            if (res == JOptionPane.OK_OPTION) {
                Cuenta c = (Cuenta) cmbCuentas.getSelectedItem();
                if (c != null) {
                    BigDecimal totalAbonado = albaranController.liquidarAlbaranEnCuenta(albaranId, c.getId());
                    JOptionPane.showMessageDialog(this,
                        "Liquidación completada exitosamente.\nSe acreditaron Q " + totalAbonado
                        + " a la cuenta " + c.getNumero() + " del socio.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        txtNumeroTalonario.setText("");
        txtCantidadAceptada.setText("0.00");
        txtCantidadRechazada.setText("0.00");
        listaLineas.clear();
        modeloLineas.setRowCount(0);
        recalcularTotalAlbaran();
    }
}