package com.example;

import java.sql.SQLException;

public class App {
    public static void main(String[] args) {
        try {
            DatabaseManager dbManager = new DatabaseManager();
            FileProcessor fileProcessor = new FileProcessor(dbManager);
            
            String folderPath = "test";
            fileProcessor.processFolder(folderPath);
            
            dbManager.closeConnection();
        } catch (SQLException e) {
            System.err.println("操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
