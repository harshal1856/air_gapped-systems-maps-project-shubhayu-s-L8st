// Data Classes for Map Analysis System
package com.secure.mapanalysis;

import java.security.MessageDigest;
import java.util.Date;

/**
 * Data class representing a map with its metadata and image data
 */
class MapData {
    private int id;
    private String name;
    private String description;
    private byte[] imageData;
    private double boundsNorth;
    private double boundsSouth;
    private double boundsEast;
    private double boundsWest;
    private double scaleFactor;
    private Date createdDate;
    private Date modifiedDate;
    private String checksum;
    
    public MapData() {
        this.createdDate = new Date();
        this.modifiedDate = new Date();
        this.scaleFactor = 1.0;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name;
        this.modifiedDate = new Date();
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description;
        this.modifiedDate = new Date();
    }
    
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { 
        this.imageData = imageData;
        this.modifiedDate = new Date();
    }
    
    public double getBoundsNorth() { return boundsNorth; }
    public void setBoundsNorth(double boundsNorth) { this.boundsNorth = boundsNorth; }
    
    public double getBoundsSouth() { return boundsSouth; }
    public void setBoundsSouth(double boundsSouth) { this.boundsSouth = boundsSouth; }
    
    public double getBoundsEast() { return boundsEast; }
    public void setBoundsEast(double boundsEast) { this.boundsEast = boundsEast; }
    
    public double getBoundsWest() { return boundsWest; }
    public void setBoundsWest(double boundsWest) { this.boundsWest = boundsWest; }
    
    public double getScaleFactor() { return scaleFactor; }
    public void setScaleFactor(double scaleFactor) { this.scaleFactor = scaleFactor; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    /**
     * Calculate SHA-256 checksum of the map data for integrity verification
     */
    public String calculateChecksum() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Include all relevant data in checksum calculation
            if (name != null) digest.update(name.getBytes());
            if (description != null) digest.update(description.getBytes());
            if (imageData != null) digest.update(imageData);
            
            digest.update(Double.toString(boundsNorth).getBytes());
            digest.update(Double.toString(boundsSouth).getBytes());
            digest.update(Double.toString(boundsEast).getBytes());
            digest.update(Double.toString(boundsWest).getBytes());
            digest.update(Double.toString(scaleFactor).getBytes());
            
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            this.checksum = hexString.toString();
            return this.checksum;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Verify integrity of map data against stored checksum
     */
    public boolean verifyIntegrity() {
        String currentChecksum = calculateChecksum();
        return currentChecksum != null && currentChecksum.equals(this.checksum);
    }
    
    @Override
    public String toString() {
        return name != null ? name : "Unnamed Map";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MapData mapData = (MapData) obj;
        return id == mapData.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}

/**
 * Data class representing a point on the map
 */
class MapPoint {
    private int id;
    private int mapId;
    private String name;
    private double latitude;
    private double longitude;
    private String symbolType;
    private String color;
    private String description;
    private Date createdDate;
    
    public MapPoint() {
        this.createdDate = new Date();
        this.symbolType = "Circle";
        this.color = "Red";
    }
    
    public MapPoint(String name, double latitude, double longitude) {
        this();
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getMapId() { return mapId; }
    public void setMapId(int mapId) { this.mapId = mapId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public String getSymbolType() { return symbolType; }
    public void setSymbolType(String symbolType) { this.symbolType = symbolType; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    /**
     * Calculate distance to another point using Haversine formula
     */
    public double distanceTo(MapPoint other) {
        return GeoUtils.calculateDistance(this.latitude, this.longitude, 
                                        other.latitude, other.longitude);
    }
    
    /**
     * Calculate bearing to another point
     */
    public double bearingTo(MapPoint other) {
        return GeoUtils.calculateBearing(this.latitude, this.longitude,
                                       other.latitude, other.longitude);
    }
    
    @Override
    public String toString() {
        return name != null ? name : String.format("Point (%.3f, %.3f)", latitude, longitude);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MapPoint mapPoint = (MapPoint) obj;
        return id == mapPoint.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}

/**
 * Data class for storing distance calculations between points
 */
class DistanceCalculation {
    private int id;
    private int mapId;
    private int point1Id;
    private int point2Id;
    private double distance;
    private String unit;
    private Date calculationDate;
    private String notes;
    
    public DistanceCalculation() {
        this.calculationDate = new Date();
        this.unit = "km";
    }
    
    public DistanceCalculation(int mapId, int point1Id, int point2Id, double distance) {
        this();
        this.mapId = mapId;
        this.point1Id = point1Id;
        this.point2Id = point2Id;
        this.distance = distance;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getMapId() { return mapId; }
    public void setMapId(int mapId) { this.mapId = mapId; }
    
    public int getPoint1Id() { return point1Id; }
    public void setPoint1Id(int point1Id) { this.point1Id = point1Id; }
    
    public int getPoint2Id() { return point2Id; }
    public void setPoint2Id(int point2Id) { this.point2Id = point2Id; }
    
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public Date getCalculationDate() { return calculationDate; }
    public void setCalculationDate(Date calculationDate) { this.calculationDate = calculationDate; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    /**
     * Convert distance to different units
     */
    public double getDistanceInUnit(String targetUnit) {
        double kmDistance = distance;
        
        // Convert current unit to km first
        switch (unit.toLowerCase()) {
            case "m":
            case "meters":
                kmDistance = distance / 1000.0;
                break;
            case "mi":
            case "miles":
                kmDistance = distance * 1.60934;
                break;
            case "nm":
            case "nautical miles":
                kmDistance = distance * 1.852;
                break;
            case "ft":
            case "feet":
                kmDistance = distance * 0.0003048;
                break;
        }
        
        // Convert from km to target unit
        switch (targetUnit.toLowerCase()) {
            case "m":
            case "meters":
                return kmDistance * 1000.0;
            case "mi":
            case "miles":
                return kmDistance / 1.60934;
            case "nm":
            case "nautical miles":
                return kmDistance / 1.852;
            case "ft":
            case "feet":
                return kmDistance / 0.0003048;
            case "km":
            case "kilometers":
            default:
                return kmDistance;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Distance: %.2f %s", distance, unit);
    }
}

/**
 * Data class for representing map paths
 */
class MapPath {
    private int id;
    private int mapId;
    private String name;
    private java.util.List<MapPoint> pathPoints;
    private String color;
    private int width;
    private String style;
    private double totalDistance;
    private Date createdDate;
    
    public MapPath() {
        this.pathPoints = new java.util.ArrayList<>();
        this.createdDate = new Date();
        this.color = "Blue";
        this.width = 2;
        this.style = "solid";
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getMapId() { return mapId; }
    public void setMapId(int mapId) { this.mapId = mapId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public java.util.List<MapPoint> getPathPoints() { return pathPoints; }
    public void setPathPoints(java.util.List<MapPoint> pathPoints) { 
        this.pathPoints = pathPoints;
        calculateTotalDistance();
    }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    
    public double getTotalDistance() { return totalDistance; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    /**
     * Add a point to the path
     */
    public void addPoint(MapPoint point) {
        pathPoints.add(point);
        calculateTotalDistance();
    }
    
    /**
     * Remove a point from the path
     */
    public void removePoint(MapPoint point) {
        pathPoints.remove(point);
        calculateTotalDistance();
    }
    
    /**
     * Calculate total distance of the path
     */
    public void calculateTotalDistance() {
        totalDistance = 0.0;
        
        for (int i = 1; i < pathPoints.size(); i++) {
            MapPoint p1 = pathPoints.get(i - 1);
            MapPoint p2 = pathPoints.get(i);
            totalDistance += p1.distanceTo(p2);
        }
    }
    
    /**
     * Get path segment distances
     */
    public java.util.List<Double> getSegmentDistances() {
        java.util.List<Double> distances = new java.util.ArrayList<>();
        
        for (int i = 1; i < pathPoints.size(); i++) {
            MapPoint p1 = pathPoints.get(i - 1);
            MapPoint p2 = pathPoints.get(i);
            distances.add(p1.distanceTo(p2));
        }
        
        return distances;
    }
    
    @Override
    public String toString() {
        return name != null ? name : 
            String.format("Path (%d points, %.2f km)", pathPoints.size(), totalDistance);
    }
}