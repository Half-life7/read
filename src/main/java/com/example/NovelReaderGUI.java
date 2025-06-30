package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.border.TitledBorder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;

import com.example.App;

public class NovelReaderGUI extends JFrame {
    private JList<String> bookList;
    private JList<String> chapterList;
    private JTextArea contentArea;
    private DefaultListModel<String> bookModel;
    private DefaultListModel<String> chapterModel;
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("加载 MySQL 驱动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private JTextField urlField;
private JButton crawlButton;

public NovelReaderGUI() {
        // 每次启动时读取文件夹中的文件到数据库
        // readFilesToDatabase();
        initUI();
    }
    
    private List<String> loadPresetWebsites() {
        List<String> websites = new ArrayList<>();
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("config.ini"));
            
            for (int i = 1; prop.containsKey("website" + i); i++) {
                websites.add(prop.getProperty("website" + i));
            }
        } catch (IOException e) {
            System.err.println("读取config.ini失败: " + e.getMessage());
            // 默认网站列表
            websites.add("https://www.qidian.com");
            websites.add("https://www.zongheng.com");
            websites.add("https://www.xxsy.net");
            websites.add("https://www.17k.com");
        }
        return websites;
    }

    private void initUI() {
        setTitle("小说阅读器");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 初始化模型
        bookModel = new DefaultListModel<>();
        chapterModel = new DefaultListModel<>();

        // 创建组件
        bookList = new JList<>(bookModel);
        chapterList = new JList<>(chapterModel);
        contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        // 设置默认字体以便后续计算行间距离
        Font defaultFont = new Font("宋体", Font.PLAIN, 12);
        contentArea.setFont(defaultFont);
        JScrollPane bookScrollPane = new JScrollPane(bookList);
        JScrollPane chapterScrollPane = new JScrollPane(chapterList);
        JScrollPane contentScrollPane = new JScrollPane(contentArea);

        // 设置布局
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, bookScrollPane, chapterScrollPane);
        leftSplit.setDividerLocation(200);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, contentScrollPane);
        mainSplit.setDividerLocation(200);
        // 添加URL输入面板
        JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        urlPanel.setBorder(new TitledBorder("网络小说爬取"));
        
        // 添加常用网站下拉列表
        List<String> presetWebsites = loadPresetWebsites();
        JComboBox<String> websiteCombo = new JComboBox<>(presetWebsites.toArray(new String[0]));
        websiteCombo.addActionListener(e -> {
            urlField.setText((String)websiteCombo.getSelectedItem());
        });
        
        urlField = new JTextField(30);
        crawlButton = new JButton("爬取");
        urlPanel.add(new JLabel("常用网站:"));
        urlPanel.add(websiteCombo);
        crawlButton.addActionListener(e -> {
            String url = urlField.getText();
            if (!url.isEmpty()) {
                try {
                    DatabaseManager dbManager = new DatabaseManager();
                    String content = new FileProcessor(dbManager).readContent(url);
                    JOptionPane.showMessageDialog(this, "爬取成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    loadBooks(); // 刷新书籍列表
                    if (bookModel.size() > 0) {
                        bookList.setSelectedIndex(0); // 自动选中第一本书
                    }
                } catch (IOException | SQLException ex) {
                    JOptionPane.showMessageDialog(this, "爬取失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        urlPanel.add(new JLabel("URL:"));
        urlPanel.add(urlField);
        urlPanel.add(crawlButton);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(urlPanel, BorderLayout.NORTH);
        mainPanel.add(mainSplit, BorderLayout.CENTER);
        add(mainPanel);

        // 加载书籍列表
        loadBooks();

        // 添加事件监听
        bookList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedBook = bookList.getSelectedValue();
                if (selectedBook != null) {
                    loadChapters(selectedBook);
                }
            }
        });

        chapterList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedBook = bookList.getSelectedValue();
                String selectedChapter = chapterList.getSelectedValue();
                if (selectedBook != null && selectedChapter != null) {
                    loadContent(selectedBook, selectedChapter);
                }
            }
        });

        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> {
            loadBooks();
            chapterModel.clear();
            contentArea.setText("");
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadBooks() {
        bookModel.clear();
        try {
            System.out.println("尝试连接数据库加载书籍列表...");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/v1?useUnicode=true&characterEncoding=UTF-8", "root", "123456");
            java.sql.Statement stmt = conn.createStatement();
            System.out.println("执行 SHOW TABLES 查询...");
            ResultSet rs = stmt.executeQuery("SHOW TABLES");
            int count = 0;
            while (rs.next()) {
                String tableName = rs.getString(1);
                System.out.println("找到书籍: " + tableName);
                bookModel.addElement(tableName);
                count++;
            }
            System.out.println("共加载 " + count + " 本书籍");
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException ex) {
            System.err.println("加载书籍列表时发生 SQL 异常: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void loadChapters(String bookName) {
        chapterModel.clear();
        try {
            System.out.println("尝试连接数据库加载书籍 " + bookName + " 的章节列表...");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/v1?useUnicode=true&characterEncoding=UTF-8", "root", "123456");
            String sql = "SELECT chapter FROM `" + bookName + "` ORDER BY " +
                "CASE " +
                "    WHEN chapter REGEXP '第[一二三四五六七八九十百千]+章' THEN " +
                "        CAST(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(SUBSTRING(chapter, 2, LOCATE('章', chapter) - 2), '一', '1'), '二', '2'), '三', '3'), '四', '4'), '五', '5'), '六', '6'), '七', '7'), '八', '8'), '九', '9'), '十', '10'), '百', '100'), '千', '1000'), '零', '0'), '两', '2'), '廿', '20'), '卅', '30') AS UNSIGNED) " +
                "    ELSE " +
                "        CAST(REGEXP_SUBSTR(chapter, '[0-9]+') AS UNSIGNED) " +
                "END, chapter COLLATE utf8mb4_unicode_ci";
            System.out.println("执行查询: " + sql);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                String chapterName = rs.getString("chapter");
                System.out.println("找到章节: " + chapterName);
                chapterModel.addElement(chapterName);
                count++;
            }
            System.out.println("书籍 " + bookName + " 共加载 " + count + " 个章节");
            rs.close();
            pstmt.close();
            conn.close();
        } catch (SQLException ex) {
            System.err.println("加载书籍 " + bookName + " 的章节列表时发生 SQL 异常: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void loadContent(String bookName, String chapterName) {
        try {
            System.out.println("尝试连接数据库加载书籍 " + bookName + " 的章节 " + chapterName + " 的内容...");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/v1?useUnicode=true&characterEncoding=UTF-8", "root", "123456");
            String sql = "SELECT content FROM `" + bookName + "` WHERE chapter = ?";
            System.out.println("执行查询: " + sql + " 参数: " + chapterName);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, chapterName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String content = rs.getString("content");
                System.out.println("成功加载章节内容，长度: " + (content != null ? content.length() : 0));
                // 处理连续两个以上空格，换行并添加首行缩进
                if (content != null) {
                    // 处理连续两个以上空格，换行并添加首行缩进
                    // 处理连续两个以上空格，换行并添加首行缩进
                    content = content.replaceAll("(\\s{2,})", "\n\n\u3000");
                    // 确保每段开头都有缩进
                    content = "\u3000" + content.replaceAll("(\n)(?!\u3000)", "\n\u3000");
                    // 确保JTextArea正确处理换行
                    contentArea.setLineWrap(true);
                    contentArea.setWrapStyleWord(true);
                    
                    // 设置行间距
                    Font font = contentArea.getFont();
                    FontMetrics metrics = contentArea.getFontMetrics(font);
                    int lineHeight = metrics.getHeight() + 5; // 默认增加5像素行间距
                    contentArea.setFont(new Font(font.getName(), font.getStyle(), font.getSize()));
                    contentArea.setMargin(new Insets(0, 0, lineHeight, 0));
                    
                    // 强制重绘文本区域
                    contentArea.revalidate();
                    contentArea.repaint();
                }
                contentArea.setText(content);
                // 滚动到顶部
                contentArea.setCaretPosition(0);
                // 设置行间距离
                contentArea.setLineWrap(true);
                contentArea.setWrapStyleWord(true);
                Font font = contentArea.getFont();
                FontMetrics metrics = contentArea.getFontMetrics(font);
                int lineHeight = metrics.getHeight() + 2; // 增加 2 像素行间距离
                contentArea.setMargin(new Insets(0, 0, lineHeight, 0));
            } else {
                System.out.println("未找到书籍 " + bookName + " 的章节 " + chapterName + " 的内容");
                contentArea.setText("");
            }
            rs.close();
            pstmt.close();
            conn.close();
        } catch (SQLException ex) {
            System.err.println("加载书籍 " + bookName + " 的章节 " + chapterName + " 的内容时发生 SQL 异常: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        App.main(args);
        SwingUtilities.invokeLater(() -> {
            new NovelReaderGUI().setVisible(true);
        });
    }
}