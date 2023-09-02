package net.jlxxw.http.spider.adapter;

import java.io.IOException;
import java.util.function.Consumer;
import net.jlxxw.http.spider.HttpSpider;
import net.jlxxw.http.spider.file.DownloadFileTools;
import net.jlxxw.http.spider.file.FileInfo;
import net.jlxxw.http.spider.proxy.ProxyRestTemplatePool;
import net.jlxxw.http.spider.util.HttpUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * @author chunyang.leng
 * @date 2023-09-01 14:02
 */

public class HttpSpiderAdapter implements HttpSpider {

    private final ProxyRestTemplatePool proxyRestTemplatePool;

    private final DownloadFileTools downloadFileTools;

    public HttpSpiderAdapter(ProxyRestTemplatePool proxyRestTemplatePool, DownloadFileTools downloadFileTools) {
        this.proxyRestTemplatePool = proxyRestTemplatePool;
        this.downloadFileTools = downloadFileTools;
    }

    /**
     * 下载文件
     *
     * @param url
     * @param headers
     * @return
     * @throws IOException
     */
    @Override
    public FileInfo downloadFile(String url, HttpHeaders headers) throws Exception {
        return downloadFileTools.download(url, headers);
    }

    /**
     * 下载 html 页面
     *
     * @param url          要访问的页面
     * @param headers      请求头
     * @param htmlConsumer html 处理者
     * @return html 内容
     * @throws IOException
     */
    @Override
    public void html(String url, HttpHeaders headers, Consumer<String> htmlConsumer) throws Exception {
        proxyRestTemplatePool.doExecute((proxy)->{
            ResponseEntity<String> exchange = HttpUtils.exchange(proxy,url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            htmlConsumer.accept(exchange.getBody());
        });
    }
}
