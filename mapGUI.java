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
        mapCanvas = new MapCanvas(this);
        JScrollPane canvasScrollPane = new JScrollPane(mapCanvas);
        canvasScrollPane.setPreferredSize(new Dimension(800, 600));
        
        centerPanel.add(toolPanel, BorderLayout.NORTH);
        centerPanel.add(canvasScrollPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
    }
    
    private void createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(300, 0));
        rightPanel.setBorder(new TitledBorder("Analysis Tools"));
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Points tab
        JPanel pointsTab = createPointsPanel();
        tabbedPane.addTab("Points", pointsTab);
        
        // Distance tab
        JPanel distanceTab = createDistancePanel();
        tabbedPane.addTab("Distance", distanceTab);
        
        // Paths tab
        JPanel pathsTab = createPathsPanel();
        tabbedPane.addTab("Paths", pathsTab);
        
        rightPanel.add(tabbedPane, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }
    
    private JPanel createPointsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Point list
        pointListModel = new DefaultListModel<>();
        pointList = new JList<>(pointListModel);
        pointList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane pointScrollPane = new JScrollPane(pointList);
        
        // Point details
        JPanel pointDetailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        pointDetailsPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        pointNameField = new JTextField(15);
        pointDetailsPanel.add(pointNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        pointDetailsPanel.add(new JLabel("Symbol:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        symbolTypeCombo = new JComboBox<>(new String[]{"Circle", "Square", "Triangle", "Star", "Cross"});
        pointDetailsPanel.add(symbolTypeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        pointDetailsPanel.add(new JLabel("Color:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        colorCombo = new JComboBox<>(new String[]{"Red", "Blue", "Green", "Yellow", "Black", "White"});
        pointDetailsPanel.add(colorCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.NORTHWEST;
        pointDetailsPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.fill = GridBagConstraints.BOTH;
        pointDescriptionArea = new JTextArea(3, 15);
        pointDescriptionArea.setLineWrap(true);
        pointDescriptionArea.setWrapStyleWord(true);
        pointDetailsPanel.add(new JScrollPane(pointDescriptionArea), gbc);
        
        // Buttons
        JPanel pointButtonPanel = new JPanel(new FlowLayout());
        JButton addPointButton = new JButton("Add Point");
        addPointButton.addActionListener(e -> addPointManually());
        JButton deletePointButton = new JButton("Delete");
        deletePointButton.addActionListener(e -> deleteSelectedPoints());
        
        pointButtonPanel.add(addPointButton);
        pointButtonPanel.add(deletePointButton);
        
        panel.add(pointScrollPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(pointDetailsPanel, BorderLayout.CENTER);
        bottomPanel.add(pointButtonPanel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createDistancePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        controlPanel.add(new JLabel("Select two points to measure distance"), gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        distanceLabel = new JLabel("Distance: Not calculated");
        distanceLabel.setBorder(BorderFactory.createEtchedBorder());
        controlPanel.add(distanceLabel, gbc);
        
        JButton calculateButton = new JButton("Calculate Distance");
        calculateButton.addActionListener(e -> calculateDistance());
        gbc.gridx = 0; gbc.gridy = 2;
        controlPanel.add(calculateButton, gbc);
        
        JButton clearButton = new JButton("Clear Selection");
        clearButton.addActionListener(e -> clearSelectedPoints());
        gbc.gridx = 0; gbc.gridy = 3;
        controlPanel.add(clearButton, gbc);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private JPanel createPathsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(new JLabel("Path Tools"), gbc);
        
        JButton startPathButton = new JButton("Start New Path");
        startPathButton.addActionListener(e -> startNewPath());
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(startPathButton, gbc);
        
        JButton finishPathButton = new JButton("Finish Path");
        finishPathButton.addActionListener(e -> finishPath());
        gbc.gridx = 0; gbc.gridy = 2;
        controlPanel.add(finishPathButton, gbc);
        
        JButton clearPathButton = new JButton("Clear Path");
        clearPathButton.addActionListener(e -> clearPath());
        gbc.gridx = 0; gbc.gridy = 3;
        controlPanel.add(clearPathButton, gbc);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel securityLabel = new JLabel("SECURE MODE - AIR GAPPED");
        securityLabel.setForeground(Color.GREEN);
        securityLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(securityLabel, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    // Event handlers
    private void loadMapsFromDatabase() {
        List<MapData> maps = databaseManager.getAllMaps();
        mapListModel.clear();
        for (MapData map : maps) {
            mapListModel.addElement(map);
        }
    }
    
    private void loadSelectedMap() {
        MapData selected = mapList.getSelectedValue();
        if (selected != null) {
            currentMap = selected;
            mapNameField.setText(selected.getName());
            mapDescriptionArea.setText(selected.getDescription());
            
            // Load map into canvas
            mapCanvas.setMapData(selected);
            
            // Load points
            loadMapPoints();
            
            statusLabel.setText("Loaded map: " + selected.getName());
        }
    }
    
    private void loadMapPoints() {
        if (currentMap != null) {
            List<MapPoint> points = databaseManager.getMapPoints(currentMap.getId());
            pointListModel.clear();
            for (MapPoint point : points) {
                pointListModel.addElement(point);
            }
            mapCanvas.setMapPoints(points);
        }
    }
    
    private void saveCurrentMap() {
        if (currentMap != null) {
            currentMap.setName(mapNameField.getText());
            currentMap.setDescription(mapDescriptionArea.getText());
            
            if (databaseManager.saveMap(currentMap)) {
                statusLabel.setText("Map saved successfully");
                loadMapsFromDatabase(); // Refresh list
            } else {
                statusLabel.setText("Error saving map");
            }
        }
    }
    
    private void addPointManually() {
        if (currentMap != null) {
            String name = pointNameField.getText().trim();
            if (!name.isEmpty()) {
                // Create point at center of canvas
                MapPoint point = new MapPoint();
                point.setMapId(currentMap.getId());
                point.setName(name);
                point.setLatitude(0.0); // Would be calculated from pixel coordinates
                point.setLongitude(0.0);
                point.setSymbolType((String) symbolTypeCombo.getSelectedItem());
                point.setColor((String) colorCombo.getSelectedItem());
                point.setDescription(pointDescriptionArea.getText());
                
                if (databaseManager.saveMapPoint(point)) {
                    loadMapPoints();
                    clearPointFields();
                    statusLabel.setText("Point added: " + name);
                } else {
                    statusLabel.setText("Error adding point");
                }
            }
        }
    }
    
    private void deleteSelectedPoints() {
        // Implementation for deleting selected points
        statusLabel.setText("Delete functionality not yet implemented");
    }
    
    private void calculateDistance() {
        if (selectedPoints.size() == 2) {
            MapPoint p1 = selectedPoints.get(0);
            MapPoint p2 = selectedPoints.get(1);
            
            double distance = GeoUtils.calculateDistance(
                p1.getLatitude(), p1.getLongitude(),
                p2.getLatitude(), p2.getLongitude()
            );
            
            distanceLabel.setText(String.format("Distance: %.2f km", distance));
            
            // Save calculation to database
            DistanceCalculation calc = new DistanceCalculation();
            calc.setMapId(currentMap.getId());
            calc.setPoint1Id(p1.getId());
            calc.setPoint2Id(p2.getId());
            calc.setDistance(distance);
            calc.setUnit("km");
            
            databaseManager.saveDistanceCalculation(calc);
            
            statusLabel.setText("Distance calculated: " + String.format("%.2f km", distance));
        } else {
            statusLabel.setText("Please select exactly two points for distance calculation");
        }
    }
    
    private void clearSelectedPoints() {
        selectedPoints.clear();
        mapCanvas.clearSelection();
        distanceLabel.setText("Distance: Not calculated");
    }
    
    private void startNewPath() {
        pathPoints.clear();
        currentTool = "DRAW_PATH";
        statusLabel.setText("Click on map to add points to path");
    }
    
    private void finishPath() {
        if (pathPoints.size() >= 2) {
            // Save path to database
            statusLabel.setText("Path saved with " + pathPoints.size() + " points");
        } else {
            statusLabel.setText("Path must have at least 2 points");
        }
        pathPoints.clear();
    }
    
    private void clearPath() {
        pathPoints.clear();
        mapCanvas.clearPath();
        statusLabel.setText("Path cleared");
    }
    
    private void clearPointFields() {
        pointNameField.setText("");
        pointDescriptionArea.setText("");
        symbolTypeCombo.setSelectedIndex(0);
        colorCombo.setSelectedIndex(0);
    }
    
    private void filterMaps() {
        // Implementation for filtering maps based on search
        String searchText = searchField.getText().toLowerCase();
        // Filter logic would go here
    }
    
    // Menu action methods
    private void importMap() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image files", "jpg", "png", "gif", "bmp"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            // Import map logic
            statusLabel.setText("Map import functionality to be implemented");
        }
    }
    
    private void exportMap() {
        if (currentMap != null) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                // Export map logic
                statusLabel.setText("Map export functionality to be implemented");
            }
        }
    }
    
    private void showDistanceCalculator() {
        // Show advanced distance calculator dialog
        statusLabel.setText("Advanced distance calculator to be implemented");
    }
    
    private void showPathAnalysis() {
        // Show path analysis dialog
        statusLabel.setText("Path analysis functionality to be implemented");
    }
    
    private void showAuditLog() {
        // Show audit log dialog
        statusLabel.setText("Audit log viewer to be implemented");
    }
    
    private void lockSystem() {
        // Lock the system - require re-authentication
        int result = JOptionPane.showConfirmDialog(this,
            "Lock the system? You will need to re-authenticate.",
            "Lock System",
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            setVisible(false);
            // Re-authentication logic would go here
        }
    }
    
    // Getters for canvas interaction
    public String getCurrentTool() {
        return currentTool;
    }
    
    public void addSelectedPoint(MapPoint point) {
        if (!selectedPoints.contains(point)) {
            selectedPoints.add(point);
            if (selectedPoints.size() > 2) {
                selectedPoints.remove(0); // Keep only last 2 points
            }
        }
    }
    
    public void addPathPoint(MapPoint point) {
        pathPoints.add(point);
    }
    
    public List<MapPoint> getSelectedPoints() {
        return selectedPoints;
    }
    
    public List<MapPoint> getPathPoints() {
        return pathPoints;
    }
    
    public void updateStatus(String status) {
        statusLabel.setText(status);
    }
}