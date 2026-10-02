package com.cooperativa.main;

import com.cooperativa.config.ConexionOracle;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        // 1. Establecer el Look and Feel del sistema operativo para Swing
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Si falla, Swing usará el Look and Feel por defecto (Metal)
        }

        // 2. Iniciar la interfaz gráfica dentro del Event Dispatch Thread (EDT) de Swing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Verificar disponibilidad de la base de datos antes de mostrar pantallas
                if (!ConexionOracle.probarConexion()) {
                    JOptionPane.showMessageDialog(
                        null,
                        "No se pudo establecer conexión con la base de datos Oracle.\n"
                        + "Por favor, verifica que Oracle XE esté activo y revisa config.properties.",
                        "Error de Conexión",
                        JOptionPane.ERROR_MESSAGE
                    );
                    System.exit(1);
                }

                System.out.println("Conexión exitosa a la base de datos.");

                // TODO: Jorge Leonidas creará e iniciará la pantalla principal o el Login
                // FrmLogin frm = new FrmLogin();
                // frm.setLocationRelativeTo(null); // Centrar en pantalla
                // frm.setVisible(true);

                JOptionPane.showMessageDialog(
                    null,
                    "¡Backend, Controllers y Base de Datos listos!\n"
                    + "Esperando la implementación de la Vista (Swing) por Jorge.",
                    "Sistema de Cooperativa Agrícola",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
    }
}