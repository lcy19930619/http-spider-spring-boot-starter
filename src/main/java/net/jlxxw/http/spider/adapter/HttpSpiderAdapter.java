package net.jlxxw.http.spider.adapter;

import java.io.IOException;
import java.util.function.Consumer;
import net.jlxxw.http.spider.HttpSpider;
import net.jlxxw.http.spider.file.DownloadFileTools;
import net.jlxxw.http.spider.file.FileInfo;
import net.jlxxw.http.spider.proxy.ProxyRestTemplateObject;
import net.jlxxw.http.spider.proxy.ProxyRestTemplatePool;
import net.jlxxw.http.spider.util.HttpUtils;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * @author chunyang.leng
 * @date 2023-09-01 14:02
 */

public class HttpSpiderAdapter implements HttpSpider {
    private static final Logger logger = LoggerFactory.getLogger(HttpSpiderAdapter.class);
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
        int i = 0;
        while ( i < 3) {
            ProxyRestTemplateObject borrow = null;
            try {
                borrow = proxyRestTemplatePool.borrow();
                ResponseEntity<String> rsp = HttpUtils.exchange(borrow,url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                String body = rsp.getBody();
                htmlConsumer.accept(body);
            }catch (HttpHostConnectException e) {
                // 个别ip访问失败者，如果是代理，直接移除
                if (borrow.isProxy()) {
                    borrow.setDelete(true);
                }
            }  catch (ResourceAccessException e) {
                Throwable cause = e.getCause();
                if (cause instanceof HttpHostConnectException) {
                    // 个别ip访问失败者，如果是代理，直接移除
                    if (borrow != null && borrow.isProxy()) {
                        borrow.setDelete(true);
                    }
                }else {
                    logger.error("下载文件产生未知异常",e);
                }
            } catch (HttpClientErrorException e) {
                HttpStatusCode statusCode = e.getStatusCode();
                if (statusCode.value() == 403 || statusCode.value() == 418) {
                    // 个别ip访问失败者，如果是代理，直接移除
                    if (borrow != null && borrow.isProxy()) {
                        borrow.setDelete(true);
                    }
                }else {
                    throw e;
                }
            }catch (Exception e) {
                i = i+1;
                logger.error("下载文html件产生未知异常,url:"+url+",正在进行重试,当前次数:" + i ,e);
            }finally {
                if (borrow != null) {
                    proxyRestTemplatePool.returnObject(borrow);
                }
            }
        }
    }
}
