package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private Connection conn;

    public DatabaseManager() throws SQLException {
        // 创建数据库
        Connection connWithoutDB = DriverManager.getConnection("jdbc:mysql://localhost:3306", "root", "123456");
        Statement stmtWithoutDB = connWithoutDB.createStatement();
        stmtWithoutDB.executeUpdate("CREATE DATABASE IF NOT EXISTS v1");
        stmtWithoutDB.close();
        connWithoutDB.close();

        // 连接到数据库
        this.conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/v1", "root", "123456");
    }

    public void createTable(String tableName) throws SQLException {
        String createTableSql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "    id INT AUTO_INCREMENT PRIMARY KEY," +
                "    chapter VARCHAR(255) NOT NULL UNIQUE," +
                "    content TEXT NOT NULL" +
                ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSql);
            System.out.println("表 " + tableName + " 创建或已存在，执行成功");
        }
    }

    public void insertChapterContent(String tableName, String chapter, String content) throws SQLException {
        String sql = "INSERT INTO `" + tableName + "` (chapter, content) VALUES (?, ?) ON DUPLICATE KEY UPDATE content = VALUES(content);";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, chapter);
            pstmt.setString(2, content);
            pstmt.executeUpdate();
        }
    }

    public void closeConnection() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    public static String sanitizeTableName(String name) {
        String sanitized = name.replaceAll("[^a-zA-Z0-9_一-龥]", "_");
        return sanitized.isEmpty() ? "table_" + System.currentTimeMillis() : sanitized;
    }
}