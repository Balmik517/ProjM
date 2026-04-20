package com.pma.spring.web.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.UserRegister;

/**
 * Legacy DAO that uses raw JDBC alongside JPA.
 * Demonstrates classic anti-patterns:
 * - Direct JDBC connections instead of connection pool
 * - Uses Hashtable and Vector (legacy thread-safe collections)
 * - Manual SQL string concatenation (SQL injection risk intentional for demo)
 * - Uses javax.annotation (removed from JDK 11)
 * - Uses commons-lang 2.x (not commons-lang3)
 */
@Repository
public class LegacyUserDao {

    private static final Logger logger = Logger.getLogger(LegacyUserDao.class);

    // Legacy thread-safe collections (unnecessary synchronization overhead)
    private static final Hashtable<Integer, UserRegister> userCache = new Hashtable<>();
    private static final Vector<String> queryLog = new Vector<>();

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired(required = false)
    private DataSource dataSource;

    private Connection sharedConnection;

    @PostConstruct
    public void initConnection() {
        try {
            // Old-style JDBC driver loading - deprecated pattern
            try {
                Class.forName("oracle.jdbc.driver.OracleDriver");
            } catch (ClassNotFoundException e) {
                logger.warn("Oracle driver not found, trying H2: " + e.getMessage());
                Class.forName("org.h2.Driver");
            }
            sharedConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            logger.info("Legacy JDBC connection established");
        } catch (Exception e) {
            logger.warn("Failed to establish legacy JDBC connection: " + e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (sharedConnection != null && !sharedConnection.isClosed()) {
                sharedConnection.close();
                logger.info("Legacy JDBC connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing connection", e);
        }
    }

    /**
     * Find user by name using raw JDBC with string concatenation.
     * Uses commons-lang 2.x StringUtils.
     */
    public UserRegister findByNameLegacy(String name) {
        // Check cache first
        for (Map.Entry<Integer, UserRegister> entry : userCache.entrySet()) {
            if (StringUtils.equals(entry.getValue().getName(), name)) {
                logger.info("Cache hit for user: " + name);
                return entry.getValue();
            }
        }

        String sql = "SELECT * FROM USER_REGISTER WHERE NAME = '" + name + "'";
        logQuery(sql);

        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                UserRegister user = mapResultSetToUser(rs);
                userCache.put(user.getId(), user);
                return user;
            }
        } catch (SQLException e) {
            logger.error("Error finding user by name: " + name, e);
        }
        return null;
    }

    /**
     * Find all users using legacy JDBC.
     */
    public List<UserRegister> findAllLegacy() {
        List<UserRegister> users = new ArrayList<>();
        String sql = "SELECT * FROM USER_REGISTER ORDER BY ID";
        logQuery(sql);

        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                UserRegister user = mapResultSetToUser(rs);
                users.add(user);
                userCache.put(user.getId(), user);
            }
        } catch (SQLException e) {
            logger.error("Error fetching all users", e);
        }
        return users;
    }

    /**
     * Count users using legacy aggregate query.
     */
    public int countUsers() {
        String sql = "SELECT COUNT(*) FROM USER_REGISTER";
        logQuery(sql);
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting users", e);
        }
        return 0;
    }

    /**
     * Bulk update using raw JDBC batch.
     */
    public void bulkUpdateStatus(List<Integer> userIds, String status) {
        String sql = "UPDATE USER_REGISTER SET ADDRESS = ? WHERE ID = ?";
        logQuery(sql);

        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (Integer id : userIds) {
                pstmt.setString(1, status);
                pstmt.setInt(2, id);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);

            // Invalidate cache
            for (Integer id : userIds) {
                userCache.remove(id);
            }
        } catch (SQLException e) {
            logger.error("Bulk update failed", e);
            try {
                getConnection().rollback();
            } catch (SQLException ex) {
                logger.error("Rollback failed", ex);
            }
        }
    }

    /**
     * Get query history.
     */
    public Vector<String> getQueryLog() {
        return queryLog;
    }

    public void clearCache() {
        userCache.clear();
        logger.info("User DAO cache cleared");
    }

    private Connection getConnection() throws SQLException {
        if (sharedConnection == null || sharedConnection.isClosed()) {
            sharedConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }
        return sharedConnection;
    }

    private UserRegister mapResultSetToUser(ResultSet rs) throws SQLException {
        UserRegister user = new UserRegister();
        user.setId(rs.getInt("ID"));
        user.setName(rs.getString("NAME"));
        user.setAddress(rs.getString("ADDRESS"));
        user.setPhoneNumber(rs.getString("PHONE_NUMBER"));
        user.setEmail(rs.getString("EMAIL"));
        user.setPassword(rs.getString("PASSWORD"));
        return user;
    }

    private void logQuery(String sql) {
        queryLog.add(System.currentTimeMillis() + " | " + sql);
        logger.debug("Executing SQL: " + sql);
    }
}
