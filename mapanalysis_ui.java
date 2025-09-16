// MapAnalysisGUI.java - Main User Interface
package com.secure.mapanalysis;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Main GUI for the Map Analysis System
 * Provides comprehensive map editing and analysis capabilities
 */
public class MapAnalysisGUI extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MapAnalysisGUI.class.getName());
    
    private DatabaseManager databaseManager;
    private SecurityManager securityManager;
    private MapCanvas mapCanvas;
    private MapData currentMap;
    
    // GUI Components
    private JList<MapData> mapList;
    private DefaultListModel<MapData> mapListModel;
    private JTextField mapNameField;
    private JTextArea mapDescriptionArea;
    private JTextField pointNameField;
    private JComboBox<String> symbolTypeCombo;
    private JComboBox<String> colorCombo;
    private JTextArea pointDescriptionArea;
    private JLabel statusLabel;
    private JPanel toolPanel;
    private JTextField searchField;
    private JList<MapPoint> pointList;
    private DefaultListModel<MapPoint> pointListModel;
    private JLabel distanceLabel;
    
    // Tool states
    private String currentTool = "SELECT";
    private List<MapPoint> selectedPoints = new ArrayList<>();
    private List<MapPoint> pathPoints = new ArrayList<>();
    
    public MapAnalysisGUI(DatabaseManager databaseManager, SecurityManager securityManager) {
        this.databaseManager = databaseManager;
        this.securityManager = securityManager;
        
        initializeGUI();
        loadMapsFromDatabase();
    }
    
    private void initializeGUI() {
        setTitle("Secure Map Analysis System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // Create main layout
        setLayout(new BorderLayout());
        
        // Create menu bar
        createMenuBar();
        
        // Create main panels
        createLeftPanel();
        createCenterPanel();
        createRightPanel();
        createBottomPanel();
        
        // Set initial status
        statusLabel.setText("Ready - Select a map to begin analysis");
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem importMapItem = new JMenuItem("Import Map");
        importMapItem.addActionListener(e -> importMap());
        fileMenu.add(importMapItem);
        
        JMenuItem exportMapItem = new JMenuItem("Export Map");
        exportMapItem.addActionListener(e -> exportMap());
        fileMenu.add(exportMapItem);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        // Tools menu
        JMenu toolsMenu = new JMenu("Tools");
        
        JMenuItem distanceCalcItem = new JMenuItem("Distance Calculator");
        distanceCalcItem.addActionListener(e -> showDistanceCalculator());
        toolsMenu.add(distanceCalcItem);
        
        JMenuItem pathAnalysisItem = new JMenuItem("Path Analysis");
        pathAnalysisItem.addActionListener(e -> showPathAnalysis());
        toolsMenu.add(pathAnalysisItem);
        
        // Security menu
        JMenu securityMenu = new JMenu("Security");
        
        JMenuItem auditLogItem = new JMenuItem("View Audit Log");
        auditLogItem.addActionListener(e -> showAuditLog());
        securityMenu.add(auditLogItem);
        
        JMenuItem lockSystemItem = new JMenuItem("Lock System");
        lockSystemItem.addActionListener(e -> lockSystem());
        securityMenu.add(lockSystemItem);
        
        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        menuBar.add(securityMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(300, 0));
        leftPanel.setBorder(new TitledBorder("Maps"));
        
        // Search field
        searchField = new JTextField();
        searchField.addActionListener(e -> filterMaps());
        
        // Map list
        mapListModel = new DefaultListModel<>();
        mapList = new JList<>(mapListModel);
        mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mapList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedMap();
            }
        });
        
        JScrollPane mapScrollPane = new JScrollPane(mapList);
        
        // Map details panel
        JPanel mapDetailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        mapDetailsPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mapNameField = new JTextField(15);
        mapDetailsPanel.add(mapNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.NORTHWEST;
        mapDetailsPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        mapDescriptionArea = new JTextArea(3, 15);
        mapDescriptionArea.setLineWrap(true);
        mapDescriptionArea.setWrapStyleWord(true);
        mapDetailsPanel.add(new JScrollPane(mapDescriptionArea), gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveMapButton = new JButton("Save Map");
        saveMapButton.addActionListener(e -> saveCurrentMap());
        buttonPanel.add(saveMapButton);
        
        leftPanel.add(new JLabel("Search:"), BorderLayout.NORTH);
        leftPanel.add(searchField, BorderLayout.NORTH);
        leftPanel.add(mapScrollPane, BorderLayout.CENTER);
        
        JPanel bottomLeft = new JPanel(new BorderLayout());
        bottomLeft.add(mapDetailsPanel, BorderLayout.CENTER);
        bottomLeft.add(buttonPanel, BorderLayout.SOUTH);
        leftPanel.add(bottomLeft, BorderLayout.SOUTH);
        
        add(leftPanel, BorderLayout.WEST);
    }
    
    private void createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new TitledBorder("Map Canvas"));
        
        // Tool panel
        toolPanel = new JPanel(new FlowLayout());
        
        JToggleButton selectTool = new JToggleButton("Select");
        selectTool.setSelected(true);
        selectTool.addActionListener(e -> currentTool = "SELECT");
        
        JToggleButton pointTool = new JToggleButton("Add Point");
        pointTool.addActionListener(e -> currentTool = "ADD_POINT");
        
        JToggleButton pathTool = new JToggleButton("Draw Path");
        pathTool.addActionListener(e -> currentTool = "DRAW_PATH");
        
        JToggleButton measureTool = new JToggleButton("Measure");
        measureTool.addActionListener(e -> currentTool = "MEASURE");
        
        ButtonGroup toolGroup = new ButtonGroup();
        toolGroup.add(selectTool);
        toolGroup.add(pointTool);
        toolGroup.add(pathTool);
        toolGroup.add(measureTool);
        
        toolPanel.add(selectTool);
        toolPanel.add(pointTool);
        toolPanel.add(pathTool);
        toolPanel.add(measureTool);
        
        // Map canvas
        mapCanvas = new MapCanvas(this