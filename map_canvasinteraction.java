// MapCanvas.java - Interactive Map Display Component
package com.secure.mapanalysis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Interactive map canvas for displaying and editing maps
 * Supports point addition, path drawing, and distance measurement
 */
public class MapCanvas extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(MapCanvas.class.getName());
    
    private MapAnalysisGUI parentGUI;
    private MapData mapData;
    private BufferedImage mapImage;
    private List<MapPoint> mapPoints = new ArrayList<>();
    private List<MapPoint> selectedPoints = new ArrayList<>();
    private List<Point> pathPoints = new ArrayList<>();
    
    // Canvas properties
    private double zoomFactor = 1.0;
    private Point panOffset = new Point(0, 0);
    private Point lastMousePosition = new Point();
    private boolean isDragging = false;
    
    // Colors and styles
    private final Color POINT_COLOR = Color.RED;
    private final Color SELECTED_COLOR = Color.BLUE;
    private final Color PATH_COLOR = Color.GREEN;
    private final int POINT_SIZE = 8;
    
    public MapCanvas(MapAnalysisGUI parentGUI) {
        this.parentGUI = parentGUI;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.LIGHT_GRAY);
        
        setupMouseListeners();
    }
    
    private void setupMouseListeners() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClicked(e);
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    private void handleMousePressed(MouseEvent e) {
        lastMousePosition = e.getPoint();
        
        if (SwingUtilities.isRightMouseButton(e)) {
            showContextMenu(e.getPoint());
        } else if ("SELECT".equals(parentGUI.getCurrentTool())) {
            isDragging = true;
        }
    }
    
    private void handleMouseReleased(MouseEvent e) {
        isDragging = false;
    }
    
    private void handleMouseDragged(MouseEvent e) {
        if (isDragging && "SELECT".equals(parentGUI.getCurrentTool())) {
            // Pan the map
            Point current = e.getPoint();
            panOffset.x += current.x - lastMousePosition.x;
            panOffset.y += current.y - lastMousePosition.y;
            lastMousePosition = current;
            repaint();
        }
    }
    
    private void handleMouseClicked(MouseEvent e) {
        Point clickPoint = e.getPoint();
        String tool = parentGUI.getCurrentTool();
        
        switch (tool) {
            case "ADD_POINT":
                addPointAtLocation(clickPoint);
                break;
                
            case "DRAW_PATH":
                addPathPoint(clickPoint);
                break;
                
            case "MEASURE":
                selectPointForMeasurement(clickPoint);
                break;
                
            case "SELECT":
                if (e.getClickCount() == 2) {
                    // Double-click to select point
                    MapPoint clickedPoint = findPointAt(clickPoint);
                    if (clickedPoint != null) {
                        togglePointSelection(clickedPoint);
                    }
                }
                break;
        }
    }
    
    private void addPointAtLocation(Point screenPoint) {
        if (mapData == null) {
            parentGUI.updateStatus("No map loaded");
            return;
        }
        
        // Convert screen coordinates to map coordinates
        Point mapPoint = screenToMapCoordinates(screenPoint);
        
        // Create dialog for point details
        PointDetailsDialog dialog = new PointDetailsDialog(parentGUI);
        MapPoint newPoint = dialog.showDialog(mapPoint);
        
        if (newPoint != null) {
            newPoint.setMapId(mapData.getId());
            mapPoints.add(newPoint);
            repaint();
            parentGUI.updateStatus("Point added at (" + mapPoint.x + ", " + mapPoint.y + ")");
        }
    }
    
    private void addPathPoint(Point screenPoint) {
        pathPoints.add(new Point(screenPoint));
        parentGUI.addPathPoint(screenToMapPoint(screenPoint));
        repaint();
        parentGUI.updateStatus("Path point added (" + pathPoints.size() + " points)");
    }
    
    private void selectPointForMeasurement(Point screenPoint) {
        MapPoint clickedPoint = findPointAt(screenPoint);
        if (clickedPoint != null) {
            parentGUI.addSelectedPoint(clickedPoint);
            togglePointSelection(clickedPoint);
            parentGUI.updateStatus("Point selected for measurement");
        }
    }
    
    private MapPoint findPointAt(Point screenPoint) {
        for (MapPoint point : mapPoints) {
            Point screenPos = mapToScreenCoordinates(new Point(
                (int)(point.getLongitude() * 100), // Scale conversion
                (int)(point.getLatitude() * 100)
            ));
            
            double distance = screenPoint.distance(screenPos);
            if (distance <= POINT_SIZE) {
                return point;
            }
        }
        return null;
    }
    
    private void togglePointSelection(MapPoint point) {
        if (selectedPoints.contains(point)) {
            selectedPoints.remove(point);
        } else {
            selectedPoints.add(point);
            if (selectedPoints.size() > 2) {
                selectedPoints.remove(0); // Keep only last 2 points
            }
        }
        repaint();
    }
    
    private Point screenToMapCoordinates(Point screenPoint) {
        // Convert screen coordinates to map coordinates considering zoom and pan
        int mapX = (int)((screenPoint.x - panOffset.x) / zoomFactor);
        int mapY = (int)((screenPoint.y - panOffset.y) / zoomFactor);
        return new Point(mapX, mapY);
    }
    
    private Point mapToScreenCoordinates(Point mapPoint) {
        // Convert map coordinates to screen coordinates considering zoom and pan
        int screenX = (int)(mapPoint.x * zoomFactor + panOffset.x);
        int screenY = (int)(mapPoint.y * zoomFactor + panOffset.y);
        return new Point(screenX, screenY);
    }
    
    private MapPoint screenToMapPoint(Point screenPoint) {
        Point mapCoord = screenToMapCoordinates(screenPoint);
        MapPoint point = new MapPoint();
        point.setLatitude(mapCoord.y / 100.0); // Scale conversion
        point.setLongitude(mapCoord.x / 100.0);
        return point;
    }
    
    private void showContextMenu(Point location) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem zoomInItem = new JMenuItem("Zoom In");
        zoomInItem.addActionListener(e -> zoomIn(location));
        contextMenu.add(zoomInItem);
        
        JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
        zoomOutItem.addActionListener(e -> zoomOut(location));
        contextMenu.add(zoomOutItem);
        
        contextMenu.addSeparator();
        
        JMenuItem resetViewItem = new JMenuItem("Reset View");
        resetViewItem.addActionListener(e -> resetView());
        contextMenu.add(resetViewItem);
        
        JMenuItem centerViewItem = new JMenuItem("Center View");
        centerViewItem.addActionListener(e -> centerView());
        contextMenu.add(centerViewItem);
        
        contextMenu.show(this, location.x, location.y);
    }
    
    private void zoomIn(Point center) {
        if (zoomFactor < 5.0) {
            double oldZoom = zoomFactor;
            zoomFactor *= 1.2;
            
            // Adjust pan to zoom into the clicked point
            panOffset.x = (int)(center.x - (center.x - panOffset.x) * zoomFactor / oldZoom);
            panOffset.y = (int)(center.y - (center.y - panOffset.y) * zoomFactor / oldZoom);
            
            repaint();
            parentGUI.updateStatus("Zoomed in: " + String.format("%.1fx", zoomFactor));
        }
    }
    
    private void zoomOut(Point center) {
        if (zoomFactor > 0.2) {
            double oldZoom = zoomFactor;
            zoomFactor /= 1.2;
            
            // Adjust pan to zoom out from the clicked point
            panOffset.x = (int)(center.x - (center.x - panOffset.x) * zoomFactor / oldZoom);
            panOffset.y = (int)(center.y - (center.y - panOffset.y) * zoomFactor / oldZoom);
            
            repaint();
            parentGUI.updateStatus("Zoomed out: " + String.format("%.1fx", zoomFactor));
        }
    }
    
    private void resetView() {
        zoomFactor = 1.0;
        panOffset = new Point(0, 0);
        repaint();
        parentGUI.updateStatus("View reset");
    }
    
    private void centerView() {
        if (mapImage != null) {
            panOffset.x = (getWidth() - mapImage.getWidth()) / 2;
            panOffset.y = (getHeight() - mapImage.getHeight()) / 2;
            repaint();
            parentGUI.updateStatus("View centered");
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw map image
        if (mapImage != null) {
            g2d.translate(panOffset.x, panOffset.y);
            g2d.scale(zoomFactor, zoomFactor);
            g2d.drawImage(mapImage, 0, 0, null);
            
            // Reset transform for overlays
            g2d.dispose();
            g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        
        // Draw grid if no map is loaded
        if (mapImage == null) {
            drawGrid(g2d);
        }
        
        // Draw map points
        drawMapPoints(g2d);
        
        // Draw path
        drawPath(g2d);
        
        // Draw selection indicators
        drawSelectionIndicators(g2d);
        
        // Draw scale and coordinates
        drawOverlayInfo(g2d);
        
        g2d.dispose();
    }
    
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1));
        
        int gridSize = 50;
        for (int x = 0; x < getWidth(); x += gridSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += gridSize) {
            g2d.drawLine(0, y, getWidth(), y);
        }
    }
    
    private void drawMapPoints(Graphics2D g2d) {
        for (MapPoint point : mapPoints) {
            Point screenPos = mapToScreenCoordinates(new Point(
                (int)(point.getLongitude() * 100),
                (int)(point.getLatitude() * 100)
            ));
            
            // Choose color based on selection state
            Color pointColor = selectedPoints.contains(point) ? SELECTED_COLOR : POINT_COLOR;
            g2d.setColor(pointColor);
            
            // Draw point based on symbol type
            drawPointSymbol(g2d, screenPos, point.getSymbolType(), POINT_SIZE);
            
            // Draw point name
            g2d.setColor(Color.BLACK);
            g2d.drawString(point.getName(), screenPos.x + POINT_SIZE + 2, screenPos.y - 2);
        }
    }
    
    private void drawPointSymbol(Graphics2D g2d, Point position, String symbolType, int size) {
        int halfSize = size / 2;
        
        switch (symbolType) {
            case "Circle":
                g2d.fillOval(position.x - halfSize, position.y - halfSize, size, size);
                break;
                
            case "Square":
                g2d.fillRect(position.x - halfSize, position.y - halfSize, size, size);
                break;
                
            case "Triangle":
                int[] xPoints = {position.x, position.x - halfSize, position.x + halfSize};
                int[] yPoints = {position.y - halfSize, position.y + halfSize, position.y + halfSize};
                g2d.fillPolygon(xPoints, yPoints, 3);
                break;
                
            case "Star":
                drawStar(g2d, position, size);
                break;
                
            case "Cross":
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(position.x - halfSize, position.y, position.x + halfSize, position.y);
                g2d.drawLine(position.x, position.y - halfSize, position.x, position.y + halfSize);
                break;
                
            default:
                g2d.fillOval(position.x - halfSize, position.y - halfSize, size, size);
                break;
        }
    }