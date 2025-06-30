package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WebNovelCrawler {
    private static final int DEFAULT_DELAY = 3000; // 3秒间隔
    private CloseableHttpClient httpClient;
    private ObjectMapper objectMapper;
    private long lastRequestTime = 0;

    public WebNovelCrawler() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }

    public String fetchHtmlContent(String url) throws IOException {
        enforceRateLimit();
        HttpGet request = new HttpGet(url);
        return httpClient.execute(request, response -> {
            // 自动检测网页编码
            String contentType = response.getEntity().getContentType().getValue();
            String charset = "UTF-8"; // 默认UTF-8
            if (contentType != null && contentType.contains("charset=")) {
                charset = contentType.substring(contentType.indexOf("charset=") + 8);
            }
            return EntityUtils.toString(response.getEntity(), charset);
        });
    }

    public Document parseHtml(String html) {
        return Jsoup.parse(html);
    }

    public String extractNovelContent(Document doc, String contentSelector) {
        String content = doc.select(contentSelector).text();
        if (content.isEmpty()) {
            // 尝试其他常见内容选择器
            content = doc.select("div.content, div.article-content, div.read-content").text();
        }
        return content;
    }

    private void enforceRateLimit() throws IOException {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastRequestTime;
        if (elapsed < DEFAULT_DELAY) {
            try {
                TimeUnit.MILLISECONDS.sleep(DEFAULT_DELAY - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }

    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}