package com.cooperativa.view;

import com.cooperativa.controller.CatalogosController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.PersonaController;
import com.cooperativa.model.Estatus;
import com.cooperativa.model.Persona;
import com.cooperativa.model.Sede;
import com.cooperativa.model.TipoPersona;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * Panel para gestión de socios y empleados de la cooperativa.
 */
public class PanelPersonas extends JPanel {

    private final PersonaController personaController = new PersonaController();
    private final CatalogosController catalogosController = new CatalogosController();

    private Integer personaSeleccionadaId = null;

    // Componentes del formulario
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtDni;
    private JTextField txtTelefono;
    private JTextField txtDireccion;
    private JComboBox<TipoPersona> cmbTipoPersona;
    private JComboBox<Sede> cmbSede;
    private JComboBox<Estatus> cmbEstatus;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnDesactivar;
    private JButton btnRefrescar;

    // Tabla
    private JTable tblPersonas;
    private DefaultTableModel modeloTabla;

    public PanelPersonas() {
        initComponents();
        cargarCombos();
        cargarTabla();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior: Formulario de datos
        JPanel pnlFormulario = new JPanel(new GridBagLayout());
        pnlFormulario.setBorder(BorderFactory.createTitledBorder("Datos de la Persona"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 0: Nombre y Apellido
        gbc.gridx = 0; gbc.gridy = 0;
        pnlFormulario.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        txtNombre = new JTextField(15);
        pnlFormulario.add(txtNombre, gbc);

        gbc.gridx = 2;
        pnlFormulario.add(new JLabel("Apellido:"), gbc);
        gbc.gridx = 3;
        txtApellido = new JTextField(15);
        pnlFormulario.add(txtApellido, gbc);

        // Fila 1: DNI y Teléfono
        gbc.gridx = 0; gbc.gridy = 1;
        pnlFormulario.add(new JLabel("DNI / Cédula:"), gbc);
        gbc.gridx = 1;
        txtDni = new JTextField(15);
        pnlFormulario.add(txtDni, gbc);

        gbc.gridx = 2;
        pnlFormulario.add(new JLabel("Teléfono:"), gbc);
        gbc.gridx = 3;
        txtTelefono = new JTextField(15);
        pnlFormulario.add(txtTelefono, gbc);

        // Fila 2: Dirección y Tipo de Persona
        gbc.gridx = 0; gbc.gridy = 2;
        pnlFormulario.add(new JLabel("Dirección:"), gbc);
        gbc.gridx = 1;
        txtDireccion = new JTextField(15);
        pnlFormulario.add(txtDireccion, gbc);

        gbc.gridx = 2;
        pnlFormulario.add(new JLabel("Tipo de Persona:"), gbc);
        gbc.gridx = 3;
        cmbTipoPersona = new JComboBox<TipoPersona>();
        pnlFormulario.add(cmbTipoPersona, gbc);

        // Fila 3: Sede y Estatus
        gbc.gridx = 0; gbc.gridy = 3;
        pnlFormulario.add(new JLabel("Sede:"), gbc);
        gbc.gridx = 1;
        cmbSede = new JComboBox<Sede>();
        pnlFormulario.add(cmbSede, gbc);

        gbc.gridx = 2;
        pnlFormulario.add(new JLabel("Estatus:"), gbc);
        gbc.gridx = 3;
        cmbEstatus = new JComboBox<Estatus>();
        pnlFormulario.add(cmbEstatus, gbc);

        // Panel de Botones
        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnNuevo = new JButton("Nuevo");
        btnGuardar = new JButton("Guardar");
        btnDesactivar = new JButton("Desactivar");
        btnRefrescar = new JButton("Refrescar");

        btnNuevo.addActionListener(e -> limpiarFormulario());
        btnGuardar.addActionListener(e -> guardarPersona());
        btnDesactivar.addActionListener(e -> desactivarPersona());
        btnRefrescar.addActionListener(e -> {
            cargarCombos();
            cargarTabla();
        });

        pnlBotones.add(btnNuevo);
        pnlBotones.add(btnGuardar);
        pnlBotones.add(btnDesactivar);
        pnlBotones.add(btnRefrescar);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 4;
        pnlFormulario.add(pnlBotones, gbc);

        add(pnlFormulario, BorderLayout.NORTH);

        // Panel inferior: Tabla de Personas
        JPanel pnlTabla = new JPanel(new BorderLayout());
        pnlTabla.setBorder(BorderFactory.createTitledBorder("Registros de Personas"));

        modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Apellido", "DNI", "Teléfono", "Dirección", "Tipo", "Sede", "Estatus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblPersonas = new JTable(modeloTabla);
        tblPersonas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPersonas.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    seleccionarFila();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tblPersonas);
        scroll.setPreferredSize(new Dimension(800, 300));
        pnlTabla.add(scroll, BorderLayout.CENTER);

        add(pnlTabla, BorderLayout.CENTER);
    }

    private void cargarCombos() {
        try {
            List<TipoPersona> tipos = catalogosController.listarTiposPersona();
            cmbTipoPersona.setModel(new DefaultComboBoxModel<TipoPersona>(tipos.toArray(new TipoPersona[0])));

            List<Sede> sedes = catalogosController.listarSedes();
            cmbSede.setModel(new DefaultComboBoxModel<Sede>(sedes.toArray(new Sede[0])));

            List<Estatus> estatusList = catalogosController.listarEstatus();
            cmbEstatus.setModel(new DefaultComboBoxModel<Estatus>(estatusList.toArray(new Estatus[0])));
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error cargando catálogos: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void cargarTabla() {
        modeloTabla.setRowCount(0);
        try {
            List<Persona> lista = personaController.listarTodas();
            for (Persona p : lista) {
                modeloTabla.addRow(new Object[]{
                    p.getId(),
                    p.getNombre(),
                    p.getApellido(),
                    p.getDni(),
                    p.getTelefono() == null ? "" : p.getTelefono(),
                    p.getDireccion() == null ? "" : p.getDireccion(),
                    obtenerTextoTipo(p.getTipoPersonaId()),
                    obtenerTextoSede(p.getSedeId()),
                    obtenerTextoEstatus(p.getEstatusId())
                });
            }
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error al consultar personas: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String obtenerTextoTipo(int tipoId) {
        for (int i = 0; i < cmbTipoPersona.getItemCount(); i++) {
            TipoPersona t = cmbTipoPersona.getItemAt(i);
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

    private String obtenerTextoEstatus(int estatusId) {
        for (int i = 0; i < cmbEstatus.getItemCount(); i++) {
            Estatus e = cmbEstatus.getItemAt(i);
            if (e.getId() == estatusId) return e.getNombre();
        }
        return String.valueOf(estatusId);
    }

    private void seleccionarFila() {
        int fila = tblPersonas.getSelectedRow();
        if (fila >= 0) {
            personaSeleccionadaId = (Integer) modeloTabla.getValueAt(fila, 0);
            txtNombre.setText((String) modeloTabla.getValueAt(fila, 1));
            txtApellido.setText((String) modeloTabla.getValueAt(fila, 2));
            txtDni.setText((String) modeloTabla.getValueAt(fila, 3));
            txtTelefono.setText((String) modeloTabla.getValueAt(fila, 4));
            txtDireccion.setText((String) modeloTabla.getValueAt(fila, 5));

            String tipoNombre = (String) modeloTabla.getValueAt(fila, 6);
            for (int i = 0; i < cmbTipoPersona.getItemCount(); i++) {
                if (cmbTipoPersona.getItemAt(i).getNombre().equalsIgnoreCase(tipoNombre)) {
                    cmbTipoPersona.setSelectedIndex(i);
                    break;
                }
            }

            String sedeNombre = (String) modeloTabla.getValueAt(fila, 7);
            for (int i = 0; i < cmbSede.getItemCount(); i++) {
                if (cmbSede.getItemAt(i).getNombre().equalsIgnoreCase(sedeNombre)) {
                    cmbSede.setSelectedIndex(i);
                    break;
                }
            }

            String estatusNombre = (String) modeloTabla.getValueAt(fila, 8);
            for (int i = 0; i < cmbEstatus.getItemCount(); i++) {
                if (cmbEstatus.getItemAt(i).getNombre().equalsIgnoreCase(estatusNombre)) {
                    cmbEstatus.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void guardarPersona() {
        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String dni = txtDni.getText().trim();
        String tel = txtTelefono.getText().trim();
        String dir = txtDireccion.getText().trim();

        TipoPersona tipo = (TipoPersona) cmbTipoPersona.getSelectedItem();
        Sede sede = (Sede) cmbSede.getSelectedItem();
        Estatus estatus = (Estatus) cmbEstatus.getSelectedItem();

        if (tipo == null || sede == null || estatus == null) {
            JOptionPane.showMessageDialog(this, "Selecciona tipo de persona, sede y estatus válidos.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (personaSeleccionadaId == null) {
                // Registrar nueva
                Persona p = new Persona();
                p.setNombre(nombre);
                p.setApellido(apellido);
                p.setDni(dni);
                p.setTipoPersonaId(tipo.getId());
                p.setSedeId(sede.getId());
                p.setTelefono(tel);
                p.setDireccion(dir);
                p.setEstatusId(estatus.getId());
                personaController.registrar(p);
                JOptionPane.showMessageDialog(this, "Persona registrada con éxito (ID: " + p.getId() + ").",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Actualizar existente
                Persona p = personaController.obtenerPorId(personaSeleccionadaId);
                p.setNombre(nombre);
                p.setApellido(apellido);
                p.setDni(dni);
                p.setTelefono(tel);
                p.setDireccion(dir);
                p.setTipoPersonaId(tipo.getId());
                p.setSedeId(sede.getId());
                p.setEstatusId(estatus.getId());
                personaController.actualizar(p);
                JOptionPane.showMessageDialog(this, "Persona actualizada exitosamente.",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
            limpiarFormulario();
            cargarTabla();
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso de Negocio", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void desactivarPersona() {
        if (personaSeleccionadaId == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una persona de la tabla para desactivar.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resp = JOptionPane.showConfirmDialog(this,
            "¿Estás seguro de desactivar la persona seleccionada?",
            "Confirmación", JOptionPane.YES_NO_OPTION);

        if (resp == JOptionPane.YES_OPTION) {
            try {
                personaController.desactivar(personaSeleccionadaId);
                JOptionPane.showMessageDialog(this, "Persona desactivada correctamente.",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario();
                cargarTabla();
            } catch (NegocioException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limpiarFormulario() {
        personaSeleccionadaId = null;
        txtNombre.setText("");
        txtApellido.setText("");
        txtDni.setText("");
        txtTelefono.setText("");
        txtDireccion.setText("");
        if (cmbTipoPersona.getItemCount() > 0) cmbTipoPersona.setSelectedIndex(0);
        if (cmbSede.getItemCount() > 0) cmbSede.setSelectedIndex(0);
        if (cmbEstatus.getItemCount() > 0) cmbEstatus.setSelectedIndex(0);
        tblPersonas.clearSelection();
    }
}
