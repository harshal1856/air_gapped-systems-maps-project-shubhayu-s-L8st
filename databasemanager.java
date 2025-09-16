// DatabaseManager.java - Secure Database Operations
package com.secure.mapanalysis;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.SecureRandom;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Secure database manager for map data operations
 * Implements encryption for sensitive data
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_URL = "jdbc:sqlite:secure_maps.db";
    
    private Connection connection;
    private SecretKey encryptionKey;
    private SecureRandom secureRandom;
    
    public DatabaseManager(SecretKey encryptionKey) {
        this.encryptionKey = encryptionKey;
        this.secureRandom = new SecureRandom();
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try {
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            
            // Enable foreign keys and security settings
            Statement stmt = connection.createStatement();
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA secure_delete = ON");
            stmt.execute("PRAGMA journal_mode = DELETE");
            
            createTables();
            LOGGER.info("Database initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database initialization failed", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();
        
        // Maps table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS maps (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                image_data BLOB,
                bounds_north REAL,
                bounds_south REAL,
                bounds_east REAL,
                bounds_west REAL,
                scale_factor REAL,
                created_date TEXT,
                modified_date TEXT,
                checksum TEXT
            )
        """);
        
        // Map points table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS map_points (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                map_id INTEGER,
                name TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                symbol_type TEXT,
                color TEXT,
                description TEXT,
                created_date TEXT,
                FOREIGN KEY (map_id) REFERENCES maps(id) ON DELETE CASCADE
            )
        """);
        
        // Map paths table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS map_paths (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                map_id INTEGER,
                name TEXT NOT NULL,
                path_data BLOB,
                color TEXT,
                width INTEGER,
                style TEXT,
                total_distance REAL,
                created_date TEXT,
                FOREIGN KEY (map_id) REFERENCES maps(id) ON DELETE CASCADE
            )
        """);
        
        // Distance calculations table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS distance_calculations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                map_id INTEGER,
                point1_id INTEGER,
                point2_id INTEGER,
                distance REAL,
                unit TEXT,
                calculation_date TEXT,
                FOREIGN KEY (map_id) REFERENCES maps(id) ON DELETE CASCADE,
                FOREIGN KEY (point1_id) REFERENCES map_points(id) ON DELETE CASCADE,
                FOREIGN KEY (point2_id) REFERENCES map_points(id) ON DELETE CASCADE
            )
        """);
        
        // Audit log table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS audit_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                action TEXT NOT NULL,
                table_name TEXT,
                record_id INTEGER,
                user_id TEXT,
                timestamp TEXT,
                details TEXT
            )
        """);
        
        stmt.close();
    }
    
    public Connection getSecureConnection() {
        return connection;
    }
    
    // Encryption/Decryption utilities
    private byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivSpec);
        byte[] encrypted = cipher.doFinal(data);
        
        // Prepend IV to encrypted data
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        
        return result;
    }
    
    private byte[] decrypt(byte[] encryptedData) throws Exception {
        // Extract IV and encrypted data
        byte[] iv = new byte[16];
        System.arraycopy(encryptedData, 0, iv, 0, 16);
        
        byte[] encrypted = new byte[encryptedData.length - 16];
        System.arraycopy(encryptedData, 16, encrypted, 0, encrypted.length);
        
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, ivSpec);
        
        return cipher.doFinal(encrypted);
    }
    
    // Map operations
    public boolean saveMap(MapData mapData) {
        try {
            String sql = """
                INSERT INTO maps (name, description, image_data, bounds_north, 
                bounds_south, bounds_east, bounds_west, scale_factor, 
                created_date, modified_date, checksum) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, mapData.getName());
            pstmt.setString(2, mapData.getDescription());
            
            // Encrypt image data
            if (mapData.getImageData() != null) {
                byte[] encryptedImage = encrypt(mapData.getImageData());
                pstmt.setBytes(3, encryptedImage);
            } else {
                pstmt.setNull(3, Types.BLOB);
            }
            
            pstmt.setDouble(4, mapData.getBoundsNorth());
            pstmt.setDouble(5, mapData.getBoundsSouth());
            pstmt.setDouble(6, mapData.getBoundsEast());
            pstmt.setDouble(7, mapData.getBoundsWest());
            pstmt.setDouble(8, mapData.getScaleFactor());
            
            String currentTime = new Date().toString();
            pstmt.setString(9, currentTime);
            pstmt.setString(10, currentTime);
            pstmt.setString(11, mapData.calculateChecksum());
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            
            if (result > 0) {
                logAuditEvent("INSERT", "maps", null, "Map saved: " + mapData.getName());
                return true;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving map", e);
        }
        return false;
    }
    
    public List<MapData> getAllMaps() {
        List<MapData> maps = new ArrayList<>();
        
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM maps ORDER BY name");
            
            while (rs.next()) {
                MapData mapData = new MapData();
                mapData.setId(rs.getInt("id"));
                mapData.setName(rs.getString("name"));
                mapData.setDescription(rs.getString("description"));
                
                // Decrypt image data
                byte[] encryptedImage = rs.getBytes("image_data");
                if (encryptedImage != null) {
                    byte[] decryptedImage = decrypt(encryptedImage);
                    mapData.setImageData(decryptedImage);
                }
                
                mapData.setBoundsNorth(rs.getDouble("bounds_north"));
                mapData.setBoundsSouth(rs.getDouble("bounds_south"));
                mapData.setBoundsEast(rs.getDouble("bounds_east"));
                mapData.setBoundsWest(rs.getDouble("bounds_west"));
                mapData.setScaleFactor(rs.getDouble("scale_factor"));
                
                maps.add(mapData);
            }
            
            rs.close();
            stmt.close();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving maps", e);
        }
        
        return maps;
    }
    
    // Point operations
    public boolean saveMapPoint(MapPoint point) {
        try {
            String sql = """
                INSERT INTO map_points (map_id, name, latitude, longitude, 
                symbol_type, color, description, created_date) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, point.getMapId());
            pstmt.setString(2, point.getName());
            pstmt.setDouble(3, point.getLatitude());
            pstmt.setDouble(4, point.getLongitude());
            pstmt.setString(5, point.getSymbolType());
            pstmt.setString(6, point.getColor());
            pstmt.setString(7, point.getDescription());
            pstmt.setString(8, new Date().toString());
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            
            if (result > 0) {
                logAuditEvent("INSERT", "map_points", null, "Point saved: " + point.getName());
                return true;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving map point", e);
        }
        return false;
    }
    
    public List<MapPoint> getMapPoints(int mapId) {
        List<MapPoint> points = new ArrayList<>();
        
        try {
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM map_points WHERE map_id = ? ORDER BY name");
            pstmt.setInt(1, mapId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                MapPoint point = new MapPoint();
                point.setId(rs.getInt("id"));
                point.setMapId(rs.getInt("map_id"));
                point.setName(rs.getString("name"));
                point.setLatitude(rs.getDouble("latitude"));
                point.setLongitude(rs.getDouble("longitude"));
                point.setSymbolType(rs.getString("symbol_type"));
                point.setColor(rs.getString("color"));
                point.setDescription(rs.getString("description"));
                
                points.add(point);
            }
            
            rs.close();
            pstmt.close();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving map points", e);
        }
        
        return points;
    }
    
    // Distance calculation operations
    public boolean saveDistanceCalculation(DistanceCalculation calculation) {
        try {
            String sql = """
                INSERT INTO distance_calculations (map_id, point1_id, point2_id, 
                distance, unit, calculation_date) VALUES (?, ?, ?, ?, ?, ?)
            """;
            
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, calculation.getMapId());
            pstmt.setInt(2, calculation.getPoint1Id());
            pstmt.setInt(3, calculation.getPoint2Id());
            pstmt.setDouble(4, calculation.getDistance());
            pstmt.setString(5, calculation.getUnit());
            pstmt.setString(6, new Date().toString());
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            
            return result > 0;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving distance calculation", e);
        }
        return false;
    }
    
    private void logAuditEvent(String action, String tableName, Integer recordId, String details) {
        try {
            String sql = """
                INSERT INTO audit_log (action, table_name, record_id, user_id, timestamp, details) 
                VALUES (?, ?, ?, ?, ?, ?)
            """;
            
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, action);
            pstmt.setString(2, tableName);
            if (recordId != null) {
                pstmt.setInt(3, recordId);
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.setString(4, System.getProperty("user.name"));
            pstmt.setString(5, new Date().toString());
            pstmt.setString(6, details);
            
            pstmt.executeUpdate();
            pstmt.close();
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error logging audit event", e);
        }
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing database connection", e);
        }
    }
}