package com.cooperativa.view;

import com.cooperativa.config.ConexionOracle;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Ventana principal del Sistema de Gestión Integral - Cooperativa Agrícola.
 * Integra la navegación de módulos mediante pestañas.
 */
public class VentanaPrincipal extends JFrame {

    private JTabbedPane tabbedPane;
    private PanelPersonas panelPersonas;
    private PanelProductos panelProductos;
    private PanelCuentas panelCuentas;
    private PanelPrestamos panelPrestamos;
    private PanelAlbaranes panelAlbaranes;

    public VentanaPrincipal() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Sistema de Gestión Integral - Cooperativa Agrícola");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setPreferredSize(new Dimension(1080, 720));

        // Encabezado institucional
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        JLabel lblTitulo = new JLabel("COOPERATIVA AGRICOLA - SISTEMA DE GESTION INTEGRAL");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitulo.setHorizontalAlignment(SwingConstants.LEFT);

        JLabel lblSubtitulo = new JLabel("Modulo Operacional y Financiero Multi-Sede - Base de Datos I (UMG)");
        lblSubtitulo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSubtitulo.setForeground(java.awt.Color.DARK_GRAY);

        pnlHeader.add(lblTitulo, BorderLayout.NORTH);
        pnlHeader.add(lblSubtitulo, BorderLayout.SOUTH);
        getContentPane().add(pnlHeader, BorderLayout.NORTH);

        // Contenedor de pestañas
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));

        panelPersonas = new PanelPersonas();
        panelProductos = new PanelProductos();
        panelCuentas = new PanelCuentas();
        panelPrestamos = new PanelPrestamos();
        panelAlbaranes = new PanelAlbaranes();

        tabbedPane.addTab("  Personas (Socios / Empleados)  ", panelPersonas);
        tabbedPane.addTab("  Productos e Insumos  ", panelProductos);
        tabbedPane.addTab("  Cuentas y Saldos  ", panelCuentas);
        tabbedPane.addTab("  Créditos y Préstamos  ", panelPrestamos);
        tabbedPane.addTab("  Recepción y Albaranes  ", panelAlbaranes);

        // Refrescar datos al cambiar de pestaña
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            switch (idx) {
                case 0: panelPersonas.cargarTabla(); break;
                case 1: panelProductos.cargarTabla(null); break;
                case 2: panelCuentas.cargarTablaCuentas(); break;
                case 3: panelPrestamos.cargarTablaPrestamos(); break;
                case 4: panelAlbaranes.cargarTablaAlbaranes(); break;
            }
        });

        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Cerrar pool físico al salir
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ConexionOracle.cerrarTodas();
            }
        });

        pack();
        setLocationRelativeTo(null); // Centrar en pantalla
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        java.awt.EventQueue.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
