package com.cooperativa.view;

import com.cooperativa.controller.CatalogosController;
import com.cooperativa.controller.CuentaController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.PersonaController;
import com.cooperativa.dao.CuentaDAO;
import com.cooperativa.model.Cuenta;
import com.cooperativa.model.CuentaPersona;
import com.cooperativa.model.Persona;
import com.cooperativa.model.Saldo;
import com.cooperativa.model.Sede;
import com.cooperativa.model.TipoCuenta;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
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
 * Panel para apertura y administración de cuentas de ahorro y monetarias,
 * control mancomunado (múltiples personas) y operaciones de depósitos y retiros en tiempo real.
 */
public class PanelCuentas extends JPanel {

    private final CuentaController cuentaController = new CuentaController();
    private final PersonaController personaController = new PersonaController();
    private final CatalogosController catalogosController = new CatalogosController();
    private final CuentaDAO cuentaDAO = new CuentaDAO();

    // Formulario de apertura
    private JTextField txtNumeroCuenta;
    private JTextField txtSaldoInicial;
    private JComboBox<TipoCuenta> cmbTipoCuenta;
    private JComboBox<Sede> cmbSede;
    private JComboBox<Persona> cmbTitularPrincipal;

    private JButton btnAbrirCuenta;
    private JButton btnLimpiar;

    // Operaciones sobre cuenta seleccionada
    private JButton btnDepositar;
    private JButton btnRetirar;
    private JButton btnConsultarSaldo;
    private JButton btnAgregarCotitular;
    private JButton btnVerPersonas;
    private JButton btnHistorialSaldos;
    private JButton btnRefrescar;

    // Tabla de cuentas
    private JTable tblCuentas;
    private DefaultTableModel modeloCuentas;

    public PanelCuentas() {
        initComponents();
        cargarCombos();
        cargarTablaCuentas();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel Superior: Apertura de Cuentas
        JPanel pnlApertura = new JPanel(new GridBagLayout());
        pnlApertura.setBorder(BorderFactory.createTitledBorder("Apertura de Nueva Cuenta"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 0: Número de Cuenta y Titular Inicial
        gbc.gridx = 0; gbc.gridy = 0;
        pnlApertura.add(new JLabel("Número de Cuenta:"), gbc);
        gbc.gridx = 1;
        txtNumeroCuenta = new JTextField(15);
        pnlApertura.add(txtNumeroCuenta, gbc);

        gbc.gridx = 2;
        pnlApertura.add(new JLabel("Titular Principal:"), gbc);
        gbc.gridx = 3;
        cmbTitularPrincipal = new JComboBox<Persona>();
        
        // CORRECCIÓN: Actualizar dinámicamente al abrir el menú desplegable
        cmbTitularPrincipal.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                actualizarComboPersonas();
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        pnlApertura.add(cmbTitularPrincipal, gbc);

        // Fila 1: Tipo de Cuenta y Sede
        gbc.gridx = 0; gbc.gridy = 1;
        pnlApertura.add(new JLabel("Tipo de Cuenta:"), gbc);
        gbc.gridx = 1;
        cmbTipoCuenta = new JComboBox<TipoCuenta>();
        pnlApertura.add(cmbTipoCuenta, gbc);

        gbc.gridx = 2;
        pnlApertura.add(new JLabel("Sede:"), gbc);
        gbc.gridx = 3;
        cmbSede = new JComboBox<Sede>();
        pnlApertura.add(cmbSede, gbc);

        // Fila 2: Saldo Inicial y Botones
        gbc.gridx = 0; gbc.gridy = 2;
        pnlApertura.add(new JLabel("Saldo Inicial (Q):"), gbc);
        gbc.gridx = 1;
        txtSaldoInicial = new JTextField("0.00", 15);
        pnlApertura.add(txtSaldoInicial, gbc);

        JPanel pnlBotonesApertura = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnLimpiar = new JButton("Limpiar");
        btnAbrirCuenta = new JButton("Abrir Cuenta");
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnAbrirCuenta.addActionListener(e -> abrirCuenta());
        pnlBotonesApertura.add(btnLimpiar);
        pnlBotonesApertura.add(btnAbrirCuenta);

        gbc.gridx = 2; gbc.gridy = 2;
        gbc.gridwidth = 2;
        pnlApertura.add(pnlBotonesApertura, gbc);
        gbc.gridwidth = 1;

        add(pnlApertura, BorderLayout.NORTH);

        // Panel Central: Tabla y Barra de Operaciones
        JPanel pnlCentro = new JPanel(new BorderLayout(5, 5));
        pnlCentro.setBorder(BorderFactory.createTitledBorder("Cuentas Registradas y Movimientos"));

        modeloCuentas = new DefaultTableModel(
            new Object[]{"ID", "Número", "Tipo de Cuenta", "Sede", "Saldo Actual (Q)"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblCuentas = new JTable(modeloCuentas);
        tblCuentas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tblCuentas);
        scroll.setPreferredSize(new Dimension(800, 260));
        pnlCentro.add(scroll, BorderLayout.CENTER);

        // Barra inferior de operaciones
        JPanel pnlOperaciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnDepositar = new JButton("Depositar");
        btnRetirar = new JButton("Retirar");
        btnConsultarSaldo = new JButton("Consultar Saldo");
        btnAgregarCotitular = new JButton("Agregar Cotitular");
        btnVerPersonas = new JButton("Ver Titulares");
        btnHistorialSaldos = new JButton("Historial Saldos");
        btnRefrescar = new JButton("Refrescar");

        btnDepositar.addActionListener(e -> realizarDeposito());
        btnRetirar.addActionListener(e -> realizarRetiro());
        btnConsultarSaldo.addActionListener(e -> verSaldoActual());
        btnAgregarCotitular.addActionListener(e -> agregarCotitular());
        btnVerPersonas.addActionListener(e -> verPersonasDeCuenta());
        btnHistorialSaldos.addActionListener(e -> verHistorial());
        btnRefrescar.addActionListener(e -> {
            cargarCombos();
            cargarTablaCuentas();
        });

        pnlOperaciones.add(btnDepositar);
        pnlOperaciones.add(btnRetirar);
        pnlOperaciones.add(btnConsultarSaldo);
        pnlOperaciones.add(btnAgregarCotitular);
        pnlOperaciones.add(btnVerPersonas);
        pnlOperaciones.add(btnHistorialSaldos);
        pnlOperaciones.add(btnRefrescar);

        pnlCentro.add(pnlOperaciones, BorderLayout.SOUTH);

        add(pnlCentro, BorderLayout.CENTER);
    }

    private void cargarCombos() {
        try {
            List<TipoCuenta> tipos = catalogosController.listarTiposCuenta();
            cmbTipoCuenta.setModel(new DefaultComboBoxModel<TipoCuenta>(tipos.toArray(new TipoCuenta[0])));

            List<Sede> sedes = catalogosController.listarSedes();
            cmbSede.setModel(new DefaultComboBoxModel<Sede>(sedes.toArray(new Sede[0])));

            actualizarComboPersonas();
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error cargando catálogos: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarComboPersonas() {
        try {
            Persona seleccionado = (Persona) cmbTitularPrincipal.getSelectedItem();
            List<Persona> personas = personaController.listarTodas();
            cmbTitularPrincipal.setModel(new DefaultComboBoxModel<Persona>(personas.toArray(new Persona[0])));
            if (seleccionado != null) {
                for (int i = 0; i < cmbTitularPrincipal.getItemCount(); i++) {
                    if (cmbTitularPrincipal.getItemAt(i).getId() == seleccionado.getId()) {
                        cmbTitularPrincipal.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (NegocioException ex) {
            // Manejo silencioso o log
        }
    }

    public void cargarTablaCuentas() {
        modeloCuentas.setRowCount(0);
        try {
            List<Cuenta> lista = cuentaDAO.obtenerTodos();
            for (Cuenta c : lista) {
                BigDecimal saldo = cuentaController.consultarSaldo(c.getId());
                modeloCuentas.addRow(new Object[]{
                    c.getId(),
                    c.getNumero(),
                    obtenerTextoTipo(c.getTipoCuentaId()),
                    obtenerTextoSede(c.getSedeId()),
                    "Q " + (saldo != null ? saldo : "0.00")
                });
            }
        } catch (SQLException | NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error cargando cuentas: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String obtenerTextoTipo(int tipoId) {
        for (int i = 0; i < cmbTipoCuenta.getItemCount(); i++) {
            TipoCuenta t = cmbTipoCuenta.getItemAt(i);
            if (t.getId() == tipoId) return t.getNombre();
        }
        return String.valueOf(tipoId);
    }

    private String obtenerTextoSede(int sedeId) {
        for (int i = 0; i < cmbSede.getItemCount(); i++) {
            Sede s = cmbSede.getItemAt(i);
            if (s.getId() == sedeId) return s.getNombre();
        }
        return String.valueOf(sedeId);
    }

    private Integer obtenerCuentaSeleccionadaId() {
        int fila = tblCuentas.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una cuenta de la tabla.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (Integer) modeloCuentas.getValueAt(fila, 0);
    }

    private void abrirCuenta() {
        TipoCuenta tipo = (TipoCuenta) cmbTipoCuenta.getSelectedItem();
        Sede sede = (Sede) cmbSede.getSelectedItem();
        Persona titular = (Persona) cmbTitularPrincipal.getSelectedItem();

        if (tipo == null || sede == null || titular == null) {
            JOptionPane.showMessageDialog(this, "Completa todos los datos requeridos.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal saldoInicial;
        try {
            saldoInicial = new BigDecimal(txtSaldoInicial.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Monto de saldo inicial inválido.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Cuenta c = new Cuenta();
        String num = txtNumeroCuenta.getText().trim();
        if (!num.isEmpty()) {
            c.setNumero(num);
        }
        c.setTipoCuentaId(tipo.getId());
        c.setSedeId(sede.getId());

        List<CuentaPersona> personas = new ArrayList<CuentaPersona>();
        personas.add(new CuentaPersona(0, titular.getId(), true));

        try {
            cuentaController.abrirCuenta(c, personas, saldoInicial);
            JOptionPane.showMessageDialog(this, "Cuenta abierta exitosamente.\nNúmero asignado: " + c.getNumero(),
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarTablaCuentas();
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso de Negocio", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void realizarDeposito() {
        Integer cuentaId = obtenerCuentaSeleccionadaId();
        if (cuentaId == null) return;

        String input = JOptionPane.showInputDialog(this, "Ingresa el monto a depositar (Q):",
            "Depósito Bancario", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            BigDecimal monto = new BigDecimal(input.trim());
            cuentaController.depositar(cuentaId, monto);
            JOptionPane.showMessageDialog(this, "Depósito de Q" + monto + " realizado exitosamente.",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarTablaCuentas();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingresa un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void realizarRetiro() {
        Integer cuentaId = obtenerCuentaSeleccionadaId();
        if (cuentaId == null) return;

        String input = JOptionPane.showInputDialog(this, "Ingresa el monto a retirar (Q):",
            "Retiro de Fondos", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            BigDecimal monto = new BigDecimal(input.trim());
            cuentaController.retirar(cuentaId, monto);
            JOptionPane.showMessageDialog(this, "Retiro de Q" + monto + " realizado con éxito.",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarTablaCuentas();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingresa un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void verSaldoActual() {
        Integer cuentaId = obtenerCuentaSeleccionadaId();
        if (cuentaId == null) return;

        try {
            BigDecimal saldo = cuentaController.consultarSaldo(cuentaId);
            JOptionPane.showMessageDialog(this, "Saldo disponible en la cuenta: Q " + saldo,
                "Consulta de Saldo", JOptionPane.INFORMATION_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarCotitular() {
        Integer cuentaId = obtenerCuentaSeleccionadaId();
        if (cuentaId == null) return;

        try {
            List<Persona> personas = personaController.listarTodas();
            JComboBox<Persona> cmbPersonasDlg = new JComboBox<Persona>(personas.toArray(new Persona[0]));
            int res = JOptionPane.showConfirmDialog(this, cmbPersonasDlg,
                "Selecciona la persona a asociar:", JOptionPane.OK_CANCEL_OPTION);

            if (res == JOptionPane.OK_OPTION) {
                Persona p = (Persona) cmbPersonasDlg.getSelectedItem();
                if (p != null) {
                    cuentaController.agregarPersona(cuentaId, p.getId(), false);
                    JOptionPane.showMessageDialog(this, "Persona asociada exitosamente a la cuenta.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void verPersonasDeCuenta() {
        Integer cuentaId = obtenerCuentaSeleccionadaId();
        if (cuentaId == null) return;

        try {
            List<CuentaPersona> lista = cuentaController.personasDeCuenta(cuentaId);
            StringBuilder sb = new StringBuilder("Personas asociadas a la cuenta ID " + cuentaId + ":\n\n");
            for (CuentaPersona cp : lista) {
                Persona p = personaController.obtenerPorId(cp.getPersonaId());
                sb.append("• ").append(p.getNombre()).append(" ").append(p.getApellido())
                  .append(" (DNI: ").append(p.getDni()).append(")")
                  .append(cp.isTitular() ? " [TITULAR]" : " [COTITULAR]")
                  .append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Titulares de la Cuenta", JOptionPane.INFORMATION_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verHistorial() {
        Integer cuentaId = obtenerCuentaSeleccionadaId();
        if (cuentaId == null) return;

        try {
            List<Saldo> historial = cuentaController.historialSaldos(cuentaId);
            StringBuilder sb = new StringBuilder("Historial cronológico de saldos:\n\n");
            for (Saldo s : historial) {
                sb.append("• ").append(s.getFecha()).append(" -> Saldo: Q ").append(s.getMonto()).append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Auditoría de Saldos", JOptionPane.INFORMATION_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        txtNumeroCuenta.setText("");
        txtSaldoInicial.setText("0.00");
        if (cmbTipoCuenta.getItemCount() > 0) cmbTipoCuenta.setSelectedIndex(0);
        if (cmbSede.getItemCount() > 0) cmbSede.setSelectedIndex(0);
        if (cmbTitularPrincipal.getItemCount() > 0) cmbTitularPrincipal.setSelectedIndex(0);
    }
}