// MapAnalysisSystem.java - Main Application Entry Point
package com.secure.mapanalysis;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Main application class for the Air-Gapped Map Analysis System
 * Implements secure initialization and authentication
 */
public class MapAnalysisSystem {
    private static final Logger LOGGER = Logger.getLogger(MapAnalysisSystem.class.getName());
    private static final String APP_VERSION = "1.0.0-SECURE";
    
    private SecurityManager securityManager;
    private DatabaseManager databaseManager;
    private MapAnalysisGUI mainGUI;
    private SecretKey encryptionKey;
    
    public MapAnalysisSystem() {
        initializeSecurity();
        initializeLogging();
    }
    
    private void initializeSecurity() {
        try {
            // Initialize encryption key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            this.encryptionKey = keyGen.generateKey();
            
            // Initialize security manager
            this.securityManager = new SecurityManager();
            
            LOGGER.info("Security initialization completed");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Security initialization failed", e);
            System.exit(1);
        }
    }
    
    private void initializeLogging() {
        // Configure secure logging
        System.setProperty("java.util.logging.config.class", 
                          "com.secure.mapanalysis.SecureLoggingConfig");
    }
    
    public boolean authenticate() {
        AuthenticationDialog authDialog = new AuthenticationDialog();
        boolean authenticated = authDialog.showDialog();
        
        if (authenticated) {
            LOGGER.info("User authenticated successfully");
            return true;
        } else {
            LOGGER.warning("Authentication failed");
            return false;
        }
    }
    
    public void initialize() {
        try {
            // Initialize database connection
            this.databaseManager = new DatabaseManager(encryptionKey);
            Connection conn = databaseManager.getSecureConnection();
            
            if (conn == null) {
                throw new RuntimeException("Failed to establish database connection");
            }
            
            // Initialize main GUI
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
                    this.mainGUI = new MapAnalysisGUI(databaseManager, securityManager);
                    this.mainGUI.setVisible(true);
                    
                    LOGGER.info("Application initialized successfully");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "GUI initialization failed", e);
                    shutdown();
                }
            });
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Application initialization failed", e);
            shutdown();
        }
    }
    
    public void shutdown() {
        LOGGER.info("Shutting down application");
        
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        
        if (mainGUI != null) {
            mainGUI.dispose();
        }
        
        // Clear sensitive data from memory
        if (encryptionKey != null) {
            // Zero out key bytes (implementation specific)
            encryptionKey = null;
        }
        
        System.exit(0);
    }
    
    public static void main(String[] args) {
        // Set security properties for air-gapped environment
        System.setProperty("java.net.useSystemProxies", "false");
        System.setProperty("java.security.policy", "security.policy");
        
        // Disable network access
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(java.security.Permission perm) {
                if (perm instanceof java.net.SocketPermission) {
                    throw new SecurityException("Network access denied in air-gapped mode");
                }
            }
        });
        
        LOGGER.info("Starting Air-Gapped Map Analysis System v" + APP_VERSION);
        
        MapAnalysisSystem app = new MapAnalysisSystem();
        
        if (app.authenticate()) {
            app.initialize();
        } else {
            LOGGER.severe("Authentication failed - shutting down");
            System.exit(1);
        }
        
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
    }
}

/**
 * Authentication Dialog for user login
 */
class AuthenticationDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private boolean authenticated = false;
    
    public AuthenticationDialog() {
        setTitle("Secure Authentication");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Username field
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(20);
        mainPanel.add(usernameField, gbc);
        
        // Password field
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        mainPanel.add(passwordField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");
        
        loginButton.addActionListener(e -> performAuthentication());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    private void performAuthentication() {
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();
        
        // Simple authentication - in production, use proper authentication
        if ("admin".equals(username) && "secure123".equals(new String(password))) {
            authenticated = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials", "Error", 
                                        JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
        }
        
        // Clear password from memory
        java.util.Arrays.fill(password, ' ');
    }
    
    public boolean showDialog() {
        setVisible(true);
        return authenticated;
    }
}