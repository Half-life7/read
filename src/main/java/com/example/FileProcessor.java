package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.net.URL;
import com.example.WebNovelCrawler;
import org.jsoup.nodes.Document;

/**
 * 文件处理器类，负责处理文本文件的读取和内容处理
 */
public class FileProcessor {
    private DatabaseManager dbManager;

    /**
     * 构造函数
     * @param dbManager 数据库管理器实例
     */
    public FileProcessor(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * 处理指定文件夹下的所有子文件夹和文本文件
     * @param folderPath 要处理的文件夹路径
     * @throws SQLException 如果数据库操作发生错误
     */
    public void processFolder(String folderPath) throws SQLException {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            // 获取所有子文件夹
            File[] subFolders = folder.listFiles(File::isDirectory);
            if (subFolders != null) {
                // 按名称排序子文件夹
                Arrays.sort(subFolders, Comparator.comparing(File::getName));
                
                // 处理每个子文件夹
                for (File subFolder : subFolders) {
                    // 创建对应的数据库表
                    String tableName = DatabaseManager.sanitizeTableName(subFolder.getName());
                    dbManager.createTable(tableName);
                    
                    // 获取子文件夹中的所有文本文件
                    File[] chapterFiles = subFolder.listFiles((dir, name) -> name.endsWith(".txt"));
                    if (chapterFiles != null) {
                        // 按名称排序文本文件
                        Arrays.sort(chapterFiles, Comparator.comparing(File::getName));
                        
                        // 处理每个文本文件
                        for (File chapterFile : chapterFiles) {
                            processChapterFile(tableName, chapterFile);
                        }
                    }
                }
            }
        } else {
            System.out.println("Folder does not exist or is not a directory.");
        }
    }

    /**
     * 处理单个章节文件
     * @param tableName 数据库表名
     * @param chapterFile 章节文件
     */
    private void processChapterFile(String tableName, File chapterFile) {
        try {
            String content;
            if (chapterFile.getName().startsWith("http://") || chapterFile.getName().startsWith("https://")) {
                WebNovelCrawler crawler = new WebNovelCrawler();
                try {
                    String html = crawler.fetchHtmlContent(chapterFile.getName());
                    Document doc = crawler.parseHtml(html);
                    content = crawler.extractNovelContent(doc, "div.chapter-content");
                } finally {
                    crawler.close();
                }
            } else {
                content = readFileContent(chapterFile);
            }
            dbManager.insertChapterContent(tableName, chapterFile.getName(), content);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取内容，支持文件和URL
     * @param source 文件路径或URL
     * @return 内容字符串
     * @throws IOException 如果读取失败
     * @throws SQLException 如果数据库操作失败
     */
    public String readContent(String source) throws IOException, SQLException {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            WebNovelCrawler crawler = new WebNovelCrawler();
            try {
                String html = crawler.fetchHtmlContent(source);
                Document doc = crawler.parseHtml(html);
                String content = crawler.extractNovelContent(doc, "div.chapter-content");
                
                // 获取书名和章节名
                String bookName = doc.select("h1.book-title").text();
                if (bookName.isEmpty()) {
                    bookName = "未命名书籍";
                }
                // 清理表名中的非法字符
                bookName = bookName.replaceAll("[^\\w\\u4e00-\\u9fa5]", "");
                String chapterName = doc.select("h1.chapter-title").text();
                if (chapterName.isEmpty()) {
                    chapterName = "未命名章节";
                }
                
                // 保存到数据库
                dbManager.saveChapter(bookName, chapterName, content);
                
                return content;
            } finally {
                crawler.close();
            }
        } else {
            return readFileContent(new File(source));
        }
    }

    private String readFileContent(File chapterFile) throws SQLException {
        try (Reader fileReader = new InputStreamReader(new FileInputStream(chapterFile), "GB2312");
             BufferedReader reader = new BufferedReader(fileReader)) {
            String line;
            System.out.println("Reading file: " + chapterFile.getName());
            StringBuilder contentBuilder = new StringBuilder();
            
            // 逐行读取文件内容
            while ((line = reader.readLine()) != null) {
                // 过滤不需要的内容
                String filteredLine = line;
                filteredLine = filteredLine.replaceAll("&nbsp;", ""); // 去除HTML空格
                filteredLine = filteredLine.replaceAll("(app2|chaptererror)\\([^)]*\\);?", ""); // 去除特定函数调用
                filteredLine = filteredLine.replaceAll("[^\\s]\\([^)]*\\);?", ""); // 去除其他函数调用
                filteredLine = filteredLine.replaceAll("<[^>]+>", ""); // 去除HTML标签
                
                contentBuilder.append(filteredLine).append("\n");
            }
            
            return contentBuilder.toString().trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}