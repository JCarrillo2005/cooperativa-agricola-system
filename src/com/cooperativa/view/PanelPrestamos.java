package com.cooperativa.view;

import com.cooperativa.controller.CatalogosController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.PersonaController;
import com.cooperativa.controller.PrestamoController;
import com.cooperativa.controller.ProductoController;
import com.cooperativa.model.DetallePrestamo;
import com.cooperativa.model.Persona;
import com.cooperativa.model.Prestamo;
import com.cooperativa.model.Producto;
import com.cooperativa.model.TipoPrestamo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import javax.swing.table.DefaultTableModel;

/**
 * Panel para otorgamiento y seguimiento de créditos: efectivo, hipotecario
 * y préstamos de consumo de insumos agrícolas (semillas, abonos, etc.),
 * así como registro de abonos y amortizaciones.
 */
public class PanelPrestamos extends JPanel {

    private final PrestamoController prestamoController = new PrestamoController();
    private final PersonaController personaController = new PersonaController();
    private final ProductoController productoController = new ProductoController();
    private final CatalogosController catalogosController = new CatalogosController();

    // Controles Préstamo
    private JComboBox<Persona> cmbSocio;
    private JComboBox<TipoPrestamo> cmbTipoPrestamo;
    private JTextField txtMonto;
    private JTextField txtTasaInteres;
    private JComboBox<String> cmbPlazoMeses;

    // Controles para productos de consumo
    private JComboBox<Producto> cmbProductoInsumo;
    private JTextField txtCantidadInsumo;
    private JButton btnAgregarInsumo;
    private JButton btnQuitarInsumo;
    private JTable tblInsumosAgregados;
    private DefaultTableModel modeloInsumos;
    private final List<DetallePrestamo> listaDetalles = new ArrayList<DetallePrestamo>();

    private JButton btnOtorgar;
    private JButton btnLimpiar;

    // Tabla de préstamos y acciones
    private JTable tblPrestamos;
    private DefaultTableModel modeloPrestamos;
    private JButton btnAbonar;
    private JButton btnVerDetalles;
    private JButton btnRefrescar;

    public PanelPrestamos() {
        initComponents();
        cargarCombos();
        cargarTablaPrestamos();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel Superior: Formulario de Préstamo dividido en Datos Generales e Insumos
        JPanel pnlNorte = new JPanel(new BorderLayout(10, 5));
        pnlNorte.setBorder(BorderFactory.createTitledBorder("Solicitud y Otorgamiento de Préstamo"));

        // Panel Izquierdo: Datos Generales
        JPanel pnlGenerales = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        pnlGenerales.add(new JLabel("Socio Solicitante:"), gbc);
        gbc.gridx = 1;
        cmbSocio = new JComboBox<Persona>();
        pnlGenerales.add(cmbSocio, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        pnlGenerales.add(new JLabel("Tipo de Préstamo:"), gbc);
        gbc.gridx = 1;
        cmbTipoPrestamo = new JComboBox<TipoPrestamo>();
        cmbTipoPrestamo.addActionListener(e -> alternarTipoPrestamo());
        pnlGenerales.add(cmbTipoPrestamo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        pnlGenerales.add(new JLabel("Monto Solicitado (Q):"), gbc);
        gbc.gridx = 1;
        txtMonto = new JTextField("1000.00", 12);
        pnlGenerales.add(txtMonto, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        pnlGenerales.add(new JLabel("Tasa de Interés (%):"), gbc);
        gbc.gridx = 1;
        txtTasaInteres = new JTextField("5.00", 12);
        pnlGenerales.add(txtTasaInteres, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        pnlGenerales.add(new JLabel("Plazo (Vencimiento):"), gbc);
        gbc.gridx = 1;
        cmbPlazoMeses = new JComboBox<String>(new String[]{"6 Meses", "12 Meses", "24 Meses", "36 Meses"});
        pnlGenerales.add(cmbPlazoMeses, gbc);

        pnlNorte.add(pnlGenerales, BorderLayout.WEST);

        // Panel Derecho: Insumos Agrícolas (para créditos de consumo)
        JPanel pnlInsumos = new JPanel(new BorderLayout(5, 5));
        pnlInsumos.setBorder(BorderFactory.createTitledBorder("Detalle de Insumos (Semillas, Abonos, Fertilizantes)"));

        JPanel pnlBarraInsumos = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        cmbProductoInsumo = new JComboBox<Producto>();
        txtCantidadInsumo = new JTextField("1", 4);
        btnAgregarInsumo = new JButton("Agregar Insumo");
        btnQuitarInsumo = new JButton("Quitar");

        btnAgregarInsumo.addActionListener(e -> agregarInsumo());
        btnQuitarInsumo.addActionListener(e -> quitarInsumo());

        pnlBarraInsumos.add(new JLabel("Producto:"));
        pnlBarraInsumos.add(cmbProductoInsumo);
        pnlBarraInsumos.add(new JLabel("Cantidad:"));
        pnlBarraInsumos.add(txtCantidadInsumo);
        pnlBarraInsumos.add(btnAgregarInsumo);
        pnlBarraInsumos.add(btnQuitarInsumo);
        pnlInsumos.add(pnlBarraInsumos, BorderLayout.NORTH);

        modeloInsumos = new DefaultTableModel(
            new Object[]{"ID", "Insumo", "Cantidad", "Precio Unitario", "Subtotal"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblInsumosAgregados = new JTable(modeloInsumos);
        JScrollPane scrollInsumos = new JScrollPane(tblInsumosAgregados);
        scrollInsumos.setPreferredSize(new Dimension(460, 110));
        pnlInsumos.add(scrollInsumos, BorderLayout.CENTER);

        pnlNorte.add(pnlInsumos, BorderLayout.CENTER);

        // Botones Generales de Solicitud
        JPanel pnlAccionNorte = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnLimpiar = new JButton("Limpiar");
        btnOtorgar = new JButton("Otorgar Préstamo");
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnOtorgar.addActionListener(e -> otorgarPrestamo());
        pnlAccionNorte.add(btnLimpiar);
        pnlAccionNorte.add(btnOtorgar);
        pnlNorte.add(pnlAccionNorte, BorderLayout.SOUTH);

        add(pnlNorte, BorderLayout.NORTH);

        // Panel Inferior: Tabla de préstamos y acciones
        JPanel pnlSur = new JPanel(new BorderLayout(5, 5));
        pnlSur.setBorder(BorderFactory.createTitledBorder("Cartera de Préstamos Registrados"));

        modeloPrestamos = new DefaultTableModel(
            new Object[]{"ID", "Socio", "Tipo", "Saldo / Monto (Q)", "Tasa (%)", "Fecha Otorgado", "Estatus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblPrestamos = new JTable(modeloPrestamos);
        tblPrestamos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPrestamos = new JScrollPane(tblPrestamos);
        scrollPrestamos.setPreferredSize(new Dimension(800, 240));
        pnlSur.add(scrollPrestamos, BorderLayout.CENTER);

        JPanel pnlOperaciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnAbonar = new JButton("Registrar Abono / Pagar Cuota");
        btnVerDetalles = new JButton("Ver Insumos Entregados");
        btnRefrescar = new JButton("Refrescar");

        btnAbonar.addActionListener(e -> registrarAbono());
        btnVerDetalles.addActionListener(e -> verInsumosEntregados());
        btnRefrescar.addActionListener(e -> {
            cargarCombos();
            cargarTablaPrestamos();
        });

        pnlOperaciones.add(btnAbonar);
        pnlOperaciones.add(btnVerDetalles);
        pnlOperaciones.add(btnRefrescar);
        pnlSur.add(pnlOperaciones, BorderLayout.SOUTH);

        add(pnlSur, BorderLayout.CENTER);
    }

    private void cargarCombos() {
        try {
            List<TipoPrestamo> tipos = catalogosController.listarTiposPrestamo();
            cmbTipoPrestamo.setModel(new DefaultComboBoxModel<TipoPrestamo>(tipos.toArray(new TipoPrestamo[0])));

            List<Persona> socios = personaController.listarSocios();
            cmbSocio.setModel(new DefaultComboBoxModel<Persona>(socios.toArray(new Persona[0])));

            List<Producto> productos = productoController.listarTodos();
            cmbProductoInsumo.setModel(new DefaultComboBoxModel<Producto>(productos.toArray(new Producto[0])));

            alternarTipoPrestamo();
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error cargando catálogos: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alternarTipoPrestamo() {
        TipoPrestamo tipo = (TipoPrestamo) cmbTipoPrestamo.getSelectedItem();
        boolean esConsumo = (tipo != null && "CONSUMO_PRODUCTOS".equalsIgnoreCase(tipo.getNombre()));

        txtMonto.setEditable(!esConsumo);
        btnAgregarInsumo.setEnabled(esConsumo);
        btnQuitarInsumo.setEnabled(esConsumo);
        cmbProductoInsumo.setEnabled(esConsumo);
        txtCantidadInsumo.setEnabled(esConsumo);

        if (esConsumo) {
            calcularMontoDesdeInsumos();
        }
    }

    private void agregarInsumo() {
        Producto p = (Producto) cmbProductoInsumo.getSelectedItem();
        if (p == null) return;

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidadInsumo.getText().trim());
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingresa una cantidad entera mayor que cero.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DetallePrestamo det = new DetallePrestamo(p.getId(), cantidad, p.getPrecio());
        listaDetalles.add(det);

        modeloInsumos.addRow(new Object[]{
            p.getId(),
            p.getNombre(),
            cantidad,
            "Q " + p.getPrecio(),
            "Q " + det.getSubtotal()
        });

        calcularMontoDesdeInsumos();
        txtCantidadInsumo.setText("1");
    }

    private void quitarInsumo() {
        int fila = tblInsumosAgregados.getSelectedRow();
        if (fila >= 0) {
            listaDetalles.remove(fila);
            modeloInsumos.removeRow(fila);
            calcularMontoDesdeInsumos();
        } else {
            JOptionPane.showMessageDialog(this, "Selecciona una fila de la tabla de insumos.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void calcularMontoDesdeInsumos() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePrestamo d : listaDetalles) {
            total = total.add(d.getSubtotal());
        }
        txtMonto.setText(total.setScale(2, RoundingMode.HALF_UP).toString());
    }

    public void cargarTablaPrestamos() {
        modeloPrestamos.setRowCount(0);
        try {
            List<Prestamo> lista = prestamoController.listarTodos();
            for (Prestamo p : lista) {
                Persona socio = personaController.obtenerPorId(p.getPersonaId());
                String nombreSocio = (socio != null) ? socio.getNombre() + " " + socio.getApellido() : String.valueOf(p.getPersonaId());
                modeloPrestamos.addRow(new Object[]{
                    p.getId(),
                    nombreSocio,
                    obtenerTextoTipoPrestamo(p.getTipoPrestamoId()),
                    "Q " + p.getMonto(),
                    p.getTasaInteres() + " %",
                    p.getFecha(),
                    obtenerTextoEstatus(p.getEstatusId())
                });
            }
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error al listar préstamos: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String obtenerTextoTipoPrestamo(int id) {
        for (int i = 0; i < cmbTipoPrestamo.getItemCount(); i++) {
            TipoPrestamo t = cmbTipoPrestamo.getItemAt(i);
            if (t.getId() == id) return t.getNombre();
        }
        return String.valueOf(id);
    }

    private String obtenerTextoEstatus(int estatusId) {
        try {
            for (com.cooperativa.model.Estatus e : catalogosController.listarEstatus()) {
                if (e.getId() == estatusId) return e.getNombre();
            }
        } catch (Exception ignored) {}
        return String.valueOf(estatusId);
    }

    private void otorgarPrestamo() {
        Persona socio = (Persona) cmbSocio.getSelectedItem();
        TipoPrestamo tipo = (TipoPrestamo) cmbTipoPrestamo.getSelectedItem();

        if (socio == null || tipo == null) {
            JOptionPane.showMessageDialog(this, "Selecciona socio y tipo de préstamo.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal tasa;
        try {
            tasa = new BigDecimal(txtTasaInteres.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ingresa una tasa de interés válida.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Prestamo p = new Prestamo();
        p.setPersonaId(socio.getId());
        p.setTipoPrestamoId(tipo.getId());
        p.setTasaInteres(tasa);

        // Calcular fecha futura de vencimiento según plazo
        int meses = 12;
        String plazoStr = (String) cmbPlazoMeses.getSelectedItem();
        if (plazoStr != null && plazoStr.startsWith("6")) meses = 6;
        else if (plazoStr != null && plazoStr.startsWith("24")) meses = 24;
        else if (plazoStr != null && plazoStr.startsWith("36")) meses = 36;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, meses);
        p.setFechaVencimiento(cal.getTime());

        boolean esConsumo = "CONSUMO_PRODUCTOS".equalsIgnoreCase(tipo.getNombre());
        if (esConsumo) {
            if (listaDetalles.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Agrega al menos un insumo al crédito de consumo.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            p.setDetalles(new ArrayList<DetallePrestamo>(listaDetalles));
        } else {
            try {
                p.setMonto(new BigDecimal(txtMonto.getText().trim()));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ingresa un monto válido para el préstamo.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            prestamoController.registrarPrestamo(p);
            JOptionPane.showMessageDialog(this, "Préstamo concedido con éxito.\nID: " + p.getId()
                + "\nMonto Total: Q " + p.getMonto(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarTablaPrestamos();
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso de Negocio", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void registrarAbono() {
        int fila = tblPrestamos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un préstamo de la tabla para abonar.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int prestamoId = (Integer) modeloPrestamos.getValueAt(fila, 0);
        String input = JOptionPane.showInputDialog(this,
            "Ingresa el monto del abono / cuota a pagar (Q):",
            "Abono a Préstamo ID " + prestamoId, JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            BigDecimal monto = new BigDecimal(input.trim());
            BigDecimal nuevoSaldo = prestamoController.registrarAbono(prestamoId, monto);
            JOptionPane.showMessageDialog(this, "Abono procesado correctamente.\nNuevo saldo pendiente: Q " + nuevoSaldo,
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarTablaPrestamos();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingresa un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void verInsumosEntregados() {
        int fila = tblPrestamos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un préstamo de la tabla.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int prestamoId = (Integer) modeloPrestamos.getValueAt(fila, 0);
        try {
            Prestamo p = prestamoController.obtener(prestamoId);
            if (p.getDetalles() == null || p.getDetalles().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Este préstamo no es de tipo consumo (no tiene insumos físicos entregados).",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            StringBuilder sb = new StringBuilder("Insumos entregados en el préstamo #" + prestamoId + ":\n\n");
            for (DetallePrestamo d : p.getDetalles()) {
                Producto prod = productoController.obtenerPorId(d.getProductoId());
                String nombreProd = (prod != null) ? prod.getNombre() : "ID " + d.getProductoId();
                sb.append("• ").append(nombreProd)
                  .append(" | Cantidad: ").append(d.getCantidad())
                  .append(" | P. Unitario: Q ").append(d.getPrecioUnitario())
                  .append(" | Subtotal: Q ").append(d.getSubtotal())
                  .append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Detalle del Crédito", JOptionPane.INFORMATION_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        txtMonto.setText("1000.00");
        txtTasaInteres.setText("5.00");
        listaDetalles.clear();
        modeloInsumos.setRowCount(0);
        txtCantidadInsumo.setText("1");
        if (cmbTipoPrestamo.getItemCount() > 0) cmbTipoPrestamo.setSelectedIndex(0);
        alternarTipoPrestamo();
    }
}
