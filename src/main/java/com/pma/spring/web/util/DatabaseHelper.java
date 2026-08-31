package com.pma.spring.web.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Shared raw-JDBC helper that any component can use to read any table.
 *
 * It deliberately has no notion of ownership: callers pass whatever SQL they
 * want, so table access performed through this class does not appear in the
 * JPA repository graph at all.
 */
@Component
public class DatabaseHelper {

    private static final Logger logger = Logger.getLogger(DatabaseHelper.class);

    private static final Vector<String> STATEMENT_LOG = new Vector<String>();

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * Run an aggregate query returning a single numeric value.
     */
    public long queryForLong(String sql, Object... params) {
        log(sql);
        if (dataSource == null) {
            logger.warn("No DataSource available for: " + sql);
            return 0L;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(sql);
            bind(statement, params);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            logger.error("queryForLong failed: " + sql, e);
            return 0L;
        } finally {
            closeQuietly(resultSet, statement, connection);
        }
    }

    /**
     * Run an arbitrary read and return the rows as maps. Used by reporting to
     * reach across domain tables without going through their owners.
     */
    public List<Map<String, Object>> queryForList(String sql, Object... params) {
        log(sql);
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (dataSource == null) {
            logger.warn("No DataSource available for: " + sql);
            return rows;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(sql);
            bind(statement, params);
            resultSet = statement.executeQuery();

            int columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> row = new Hashtable<String, Object>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = resultSet.getObject(i);
                    if (value != null) {
                        row.put(resultSet.getMetaData().getColumnLabel(i).toLowerCase(), value);
                    }
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            logger.error("queryForList failed: " + sql, e);
        } finally {
            closeQuietly(resultSet, statement, connection);
        }
        return rows;
    }

    /**
     * Fire-and-forget DML executed outside of any repository.
     */
    public int execute(String sql, Object... params) {
        log(sql);
        if (dataSource == null) {
            logger.warn("No DataSource available for: " + sql);
            return 0;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(sql);
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("execute failed: " + sql, e);
            return 0;
        } finally {
            closeQuietly(null, statement, connection);
        }
    }

    public List<String> getStatementLog(int size) {
        List<String> out = new ArrayList<String>();
        int from = Math.max(0, STATEMENT_LOG.size() - size);
        for (int i = from; i < STATEMENT_LOG.size(); i++) {
            out.add(STATEMENT_LOG.get(i));
        }
        return out;
    }

    private void bind(PreparedStatement statement, Object[] params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    private void closeQuietly(ResultSet resultSet, PreparedStatement statement, Connection connection) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            logger.debug("ResultSet close failed", e);
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            logger.debug("Statement close failed", e);
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.debug("Connection close failed", e);
        }
    }

    private void log(String sql) {
        if (STATEMENT_LOG.size() > 500) {
            STATEMENT_LOG.remove(0);
        }
        STATEMENT_LOG.add(System.currentTimeMillis() + " | " + sql);
        logger.debug("DatabaseHelper SQL: " + sql);
    }
}
