// GeoUtils.java - Geographic Calculations and Utilities
package com.secure.mapanalysis;

/**
 * Utility class for geographic calculations including distance, bearing, and coordinate conversions
 * All calculations use secure, validated algorithms suitable for air-gapped environments
 */
public class GeoUtils {
    
    // Earth's radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    // Earth's radius in different units
    private static final double EARTH_RADIUS_MILES = 3958.756;
    private static final double EARTH_RADIUS_NAUTICAL_MILES = 3440.065;
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    
    // Conversion constants
    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;
    private static final double RADIANS_TO_DEGREES = 180.0 / Math.PI;
    
    /**
     * Calculate the great-circle distance between two points using the Haversine formula
     * This is accurate for most practical purposes and handles edge cases well
     * 
     * @param lat1 Latitude of first point in decimal degrees
     * @param lon1 Longitude of first point in decimal degrees
     * @param lat2 Latitude of second point in decimal degrees
     * @param lon2 Longitude of second point in decimal degrees
     * @return Distance in kilometers
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Validate input coordinates
        if (!isValidLatitude(lat1) || !isValidLatitude(lat2) || 
            !isValidLongitude(lon1) || !isValidLongitude(lon2)) {
            throw new IllegalArgumentException("Invalid coordinates provided");
        }
        
        // Handle identical points
        if (lat1 == lat2 && lon1 == lon2) {
            return 0.0;
        }
        
        // Convert to radians
        double lat1Rad = lat1 * DEGREES_TO_RADIANS;
        double lon1Rad = lon1 * DEGREES_TO_RADIANS;
        double lat2Rad = lat2 * DEGREES_TO_RADIANS;
        double lon2Rad = lon2 * DEGREES_TO_RADIANS;
        
        // Calculate differences
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        
        // Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Calculate the great-circle distance with specified unit
     * 
     * @param lat1 Latitude of first point in decimal degrees
     * @param lon1 Longitude of first point in decimal degrees
     * @param lat2 Latitude of second point in decimal degrees
     * @param lon2 Longitude of second point in decimal degrees
     * @param unit Unit for result ("km", "miles", "nm", "meters")
     * @return Distance in specified unit
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double earthRadius;
        
        switch (unit.toLowerCase()) {
            case "miles":
            case "mi":
                earthRadius = EARTH_RADIUS_MILES;
                break;
            case "nautical miles":
            case "nm":
                earthRadius = EARTH_RADIUS_NAUTICAL_MILES;
                break;
            case "meters":
            case "m":
                earthRadius = EARTH_RADIUS_METERS;
                break;
            case "kilometers":
            case "km":
            default:
                earthRadius = EARTH_RADIUS_KM;
                break;
        }
        
        // Use the same calculation but with different earth radius
        double lat1Rad = lat1 * DEGREES_TO_RADIANS;
        double lon1Rad = lon1 * DEGREES_TO_RADIANS;
        double lat2Rad = lat2 * DEGREES_TO_RADIANS;
        double lon2Rad = lon2 * DEGREES_TO_RADIANS;
        
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c;
    }
    
    /**
     * Calculate the initial bearing from point 1 to point 2
     * 
     * @param lat1 Latitude of first point in decimal degrees
     * @param lon1 Longitude of first point in decimal degrees
     * @param lat2 Latitude of second point in decimal degrees
     * @param lon2 Longitude of second point in decimal degrees
     * @return Initial bearing in degrees (0-360)
     */
    public static double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        // Validate coordinates
        if (!