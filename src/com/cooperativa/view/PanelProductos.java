package com.cooperativa.view;

import com.cooperativa.controller.CatalogosController;
import com.cooperativa.controller.NegocioException;
import com.cooperativa.controller.ProductoController;
import com.cooperativa.model.CategoriaProducto;
import com.cooperativa.model.Producto;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
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
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;

/**
 * Panel para gestión de productos, insumos agrícolas, precios y control de stock.
 */
public class PanelProductos extends JPanel {

    private final ProductoController productoController = new ProductoController();
    private final CatalogosController catalogosController = new CatalogosController();

    private Integer productoSeleccionadoId = null;

    // Campos formulario
    private JTextField txtNombre;
    private JTextField txtDescripcion;
    private JTextField txtPrecio;
    private JTextField txtStock;
    private JComboBox<CategoriaProducto> cmbCategoria;

    // Botones
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnAgregarStock;
    private JButton btnCambiarPrecio;
    private JButton btnStockBajo;
    private JButton btnRefrescar;

    // Tabla
    private JTable tblProductos;
    private DefaultTableModel modeloTabla;

    public PanelProductos() {
        initComponents();
        cargarCategorias();
        cargarTabla(null);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel Formulario
        JPanel pnlFormulario = new JPanel(new GridBagLayout());
        pnlFormulario.setBorder(BorderFactory.createTitledBorder("Datos del Producto / Insumo"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 0: Nombre y Categoría
        gbc.gridx = 0; gbc.gridy = 0;
        pnlFormulario.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        txtNombre = new JTextField(18);
        pnlFormulario.add(txtNombre, gbc);

        gbc.gridx = 2;
        pnlFormulario.add(new JLabel("Categoría:"), gbc);
        gbc.gridx = 3;
        cmbCategoria = new JComboBox<CategoriaProducto>();
        cmbCategoria.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                cargarCategorias();
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        pnlFormulario.add(cmbCategoria, gbc);

        // Fila 1: Precio y Stock
        gbc.gridx = 0; gbc.gridy = 1;
        pnlFormulario.add(new JLabel("Precio Unitario (Q):"), gbc);
        gbc.gridx = 1;
        txtPrecio = new JTextField(18);
        pnlFormulario.add(txtPrecio, gbc);

        gbc.gridx = 2;
        pnlFormulario.add(new JLabel("Stock en Bodega:"), gbc);
        gbc.gridx = 3;
        txtStock = new JTextField(18);
        pnlFormulario.add(txtStock, gbc);

        // Fila 2: Descripción
        gbc.gridx = 0; gbc.gridy = 2;
        pnlFormulario.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        txtDescripcion = new JTextField();
        pnlFormulario.add(txtDescripcion, gbc);
        gbc.gridwidth = 1;

        // Fila 3: Botones
        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnNuevo = new JButton("Nuevo");
        btnGuardar = new JButton("Guardar");
        btnAgregarStock = new JButton("Reabastecer Stock");
        btnCambiarPrecio = new JButton("Ajustar Precio");
        btnStockBajo = new JButton("Filtrar Stock Crítico (≤5)");
        btnRefrescar = new JButton("Ver Todos");

        btnNuevo.addActionListener(e -> limpiarFormulario());
        btnGuardar.addActionListener(e -> guardarProducto());
        btnAgregarStock.addActionListener(e -> agregarStockDialogo());
        btnCambiarPrecio.addActionListener(e -> cambiarPrecioDialogo());
        btnStockBajo.addActionListener(e -> filtrarStockBajo());
        btnRefrescar.addActionListener(e -> {
            limpiarFormulario();
            cargarTabla(null);
        });

        pnlBotones.add(btnNuevo);
        pnlBotones.add(btnGuardar);
        pnlBotones.add(btnAgregarStock);
        pnlBotones.add(btnCambiarPrecio);
        pnlBotones.add(btnStockBajo);
        pnlBotones.add(btnRefrescar);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 4;
        pnlFormulario.add(pnlBotones, gbc);

        add(pnlFormulario, BorderLayout.NORTH);

        // Panel Tabla
        JPanel pnlTabla = new JPanel(new BorderLayout());
        pnlTabla.setBorder(BorderFactory.createTitledBorder("Catálogo de Productos e Insumos"));

        modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Descripción", "Categoría", "Precio Unitario", "Stock"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblProductos = new JTable(modeloTabla);
        tblProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblProductos.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    seleccionarFila();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tblProductos);
        scroll.setPreferredSize(new Dimension(800, 300));
        pnlTabla.add(scroll, BorderLayout.CENTER);

        add(pnlTabla, BorderLayout.CENTER);
    }

    private void cargarCategorias() {
        try {
            CategoriaProducto seleccionado = (CategoriaProducto) cmbCategoria.getSelectedItem();
            List<CategoriaProducto> categorias = catalogosController.listarCategoriasProducto();
            cmbCategoria.setModel(new DefaultComboBoxModel<CategoriaProducto>(categorias.toArray(new CategoriaProducto[0])));
            if (seleccionado != null) {
                for (int i = 0; i < cmbCategoria.getItemCount(); i++) {
                    if (cmbCategoria.getItemAt(i).getId() == seleccionado.getId()) {
                        cmbCategoria.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error cargando categorías: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void cargarTabla(List<Producto> listaFiltrada) {
        modeloTabla.setRowCount(0);
        try {
            List<Producto> lista = (listaFiltrada != null) ? listaFiltrada : productoController.listarTodos();
            for (Producto p : lista) {
                modeloTabla.addRow(new Object[]{
                    p.getId(),
                    p.getNombre(),
                    p.getDescripcion() == null ? "" : p.getDescripcion(),
                    obtenerTextoCategoria(p.getCategoriaId()),
                    "Q " + p.getPrecio(),
                    p.getStock()
                });
            }
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, "Error al consultar productos: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String obtenerTextoCategoria(int catId) {
        for (int i = 0; i < cmbCategoria.getItemCount(); i++) {
            CategoriaProducto c = cmbCategoria.getItemAt(i);
            if (c.getId() == catId) return c.getNombre();
        }
        return String.valueOf(catId);
    }

    private void seleccionarFila() {
        int fila = tblProductos.getSelectedRow();
        if (fila >= 0) {
            productoSeleccionadoId = (Integer) modeloTabla.getValueAt(fila, 0);
            txtNombre.setText((String) modeloTabla.getValueAt(fila, 1));
            txtDescripcion.setText((String) modeloTabla.getValueAt(fila, 2));

            String catTexto = (String) modeloTabla.getValueAt(fila, 3);
            for (int i = 0; i < cmbCategoria.getItemCount(); i++) {
                if (cmbCategoria.getItemAt(i).getNombre().equalsIgnoreCase(catTexto)) {
                    cmbCategoria.setSelectedIndex(i);
                    break;
                }
            }

            String precioStr = modeloTabla.getValueAt(fila, 4).toString().replace("Q ", "").trim();
            txtPrecio.setText(precioStr);
            txtStock.setText(modeloTabla.getValueAt(fila, 5).toString());
            txtStock.setEditable(false);
        }
    }

    private void guardarProducto() {
        String nombre = txtNombre.getText().trim();
        String desc = txtDescripcion.getText().trim();
        String precioStr = txtPrecio.getText().trim();
        String stockStr = txtStock.getText().trim();

        CategoriaProducto cat = (CategoriaProducto) cmbCategoria.getSelectedItem();
        if (cat == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una categoría válida.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal precio;
        int stock;
        try {
            precio = new BigDecimal(precioStr);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ingresa un precio válido (ej. 25.50).",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            stock = Integer.parseInt(stockStr);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ingresa un número entero para el stock.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (productoSeleccionadoId == null) {
                Producto p = new Producto();
                p.setNombre(nombre);
                p.setDescripcion(desc);
                p.setCategoriaId(cat.getId());
                p.setPrecio(precio);
                p.setStock(stock);
                productoController.registrar(p);
                JOptionPane.showMessageDialog(this, "Producto registrado correctamente (ID: " + p.getId() + ").",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Producto p = productoController.obtenerPorId(productoSeleccionadoId);
                p.setNombre(nombre);
                p.setDescripcion(desc);
                p.setCategoriaId(cat.getId());
                p.setPrecio(precio);
                productoController.actualizar(p);
                JOptionPane.showMessageDialog(this, "Producto actualizado con éxito.",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
            limpiarFormulario();
            cargarTabla(null);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso de Negocio", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void agregarStockDialogo() {
        if (productoSeleccionadoId == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto de la tabla primero.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this,
            "Ingresa la cantidad de unidades a ingresar al stock:",
            "Reabastecer Bodega", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int cantidad = Integer.parseInt(input.trim());
            productoController.agregarStock(productoSeleccionadoId, cantidad);
            JOptionPane.showMessageDialog(this, "Stock actualizado exitosamente.",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarTabla(null);
            seleccionarFila();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Debes ingresar un número entero positivo.",
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso de Negocio", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cambiarPrecioDialogo() {
        if (productoSeleccionadoId == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto de la tabla primero.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this,
            "Ingresa el nuevo precio unitario (Q):",
            "Ajustar Precio", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            BigDecimal nuevoPrecio = new BigDecimal(input.trim());
            productoController.cambiarPrecio(productoSeleccionadoId, nuevoPrecio);
            JOptionPane.showMessageDialog(this, "Precio actualizado correctamente.",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarTabla(null);
            seleccionarFila();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Debes ingresar un valor numérico decimal válido.",
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso de Negocio", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void filtrarStockBajo() {
        try {
            List<Producto> bajo = productoController.listarStockBajo(5);
            cargarTabla(bajo);
            JOptionPane.showMessageDialog(this, "Mostrando " + bajo.size() + " productos con stock crítico (<= 5).",
                "Filtro Aplicado", JOptionPane.INFORMATION_MESSAGE);
        } catch (NegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        productoSeleccionadoId = null;
        txtNombre.setText("");
        txtDescripcion.setText("");
        txtPrecio.setText("");
        txtStock.setText("");
        txtStock.setEditable(true);
        if (cmbCategoria.getItemCount() > 0) cmbCategoria.setSelectedIndex(0);
        tblProductos.clearSelection();
    }
}