package net.jlxxw.http.spider;

import java.io.IOException;
import java.util.function.Consumer;
import net.jlxxw.http.spider.file.FileInfo;
import org.springframework.http.HttpHeaders;

/**
 * @author chunyang.leng
 * @date 2023-09-01 13:59
 */
public interface HttpSpider {

    /**
     * 下载文件
     * @param url
     * @param headers
     * @return
     * @throws IOException
     */
    FileInfo downloadFile(String url, HttpHeaders headers) throws Exception;

    /**
     * 下载 html 页面
     * @param url 要访问的页面
     * @param headers 请求头
     * @return html 内容
     * @param htmlConsumer html 处理者
     * @throws IOException
     */
    void html(String url,HttpHeaders headers, Consumer<String> htmlConsumer) throws Exception;
}
