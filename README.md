# 文本阅读器项目文档

## 主要类与接口介绍

### App.java
- **功能**：程序入口类，负责协调各模块工作
- **主要方法**：
  - `main()`: 初始化DatabaseManager和FileProcessor，启动文件处理流程

### DatabaseManager.java

- **功能**：数据库管理类，处理所有数据库相关操作
- **主要方法**：
  - `createDatabase()`: 创建数据库连接
  - `createTable()`: 创建存储章节内容的表
  - `insertChapterContent()`: 插入章节内容到数据库
  - `sanitizeTableName()`: 清理表名中的特殊字符

### FileProcessor.java

- **功能**：文件处理器类，负责文件读取和内容处理
- **主要方法**：
  - `processFolder()`: 递归处理文件夹及其子文件夹中的文本文件
  - `processChapterFile()`: 处理单个章节文件，包括内容过滤和数据库插入
  - `cleanContent()`: 使用正则表达式清理文本内容

## 项目结构

```
text-reader/
├── src/
│   ├── main/java/com/example/
│   │   ├── App.java
│   │   ├── DatabaseManager.java
│   │   └── FileProcessor.java
├── test/
│   └── 示例文本文件/
```

## 使用说明
1. 将文本文件放入test目录下
2. 运行App.java启动程序
3. 程序会自动处理文本文件并存入数据库