# 文本阅读器项目文档

## 一、功能介绍

本应用是一个多功能文本处理工具，主要功能包括：

1. **本地文本处理**
   - 递归扫描文件夹中的文本文件
   - 自动清理文本中的特殊字符和格式
   - 将文本内容存储到SQLite数据库
   - 支持中文文本的规范化处理

2. **网络小说爬取**
   - 从网页获取小说内容
   - 自动检测网页编码
   - 智能提取正文内容
   - 请求频率控制防止被封禁

3. **数据管理**
   - 自动创建数据库和表
   - 表名安全处理
   - 章节内容批量插入

## 二、重要的类及接口介绍

### 1. App (主程序类)
- **职责**：程序入口，协调各模块工作
- **关键方法**：
  - `main()`: 初始化各组件并启动处理流程
  - 负责命令行参数解析和任务分发

### 2. DatabaseManager (数据库管理)
- **职责**：所有数据库相关操作
- **核心功能**：
  - `createDatabase()`: 创建/连接数据库
  - `createTable()`: 根据书名创建章节表
  - `insertChapterContent()`: 批量插入章节内容
  - 表名安全处理机制

### 3. FileProcessor (文件处理器)
- **职责**：本地文件处理
- **核心功能**：
  - `processFolder()`: 递归处理文件夹
  - `processChapterFile()`: 单文件处理流水线
  - 内容清理和规范化

### 4. WebNovelCrawler (网络爬取)
- **职责**：网页小说内容获取
- **核心技术**：
  - HTTP客户端与HTML解析
  - 内容选择器智能匹配
  - 请求间隔控制
  - 编码自动检测

### 5. NovelReaderGUI (图形界面)
- **职责**：提供用户友好的图形界面
- **核心功能**：
  - 书籍和章节列表展示
  - 内容阅读区域
  - 网络小说爬取界面
  - 数据库内容可视化
  - 阅读设置面板：支持背景色、字体颜色、大小和行间距调节

## 项目结构

```
text-reader/
├── src/
│   ├── main/java/com/example/
│   │   ├── App.java
│   │   ├── DatabaseManager.java
│   │   ├── FileProcessor.java
│   │   ├── WebNovelCrawler.java
│   │   └── NovelReaderGUI.java
├── test/
│   └── 示例文本文件/
```

## 快速开始

1. 克隆项目
```bash
git clone https://github.com/your-repo/text-reader.git
```

2. 构建项目
```bash
mvn package
```

3. 运行程序
```bash
java -jar target/text-reader-1.0-SNAPSHOT.jar
```

或者使用批处理文件（Windows系统）：
```bash
cd run
start gui.bat
```



