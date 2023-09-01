package net.jlxxw.http.spider.file.thread;

import java.io.InputStream;
import net.jlxxw.http.spider.file.FileInfo;
import net.jlxxw.http.spider.proxy.ProxyRestTemplateObject;
import net.jlxxw.http.spider.proxy.ProxyRestTemplatePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/**
 * 下载大文件线程
 *
 * @author chunyang.leng
 * @date 2023-08-31 14:44
 */
class DownloadBigFileThread extends AbstractDownloadFileThread {
    private static final Logger logger = LoggerFactory.getLogger(DownloadBigFileThread.class);
    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY = 3;
    /**
     * 头
     */
    private final HttpHeaders header;
    /**
     * 文件信息
     */
    private final FileInfo fileInfo;

    /**
     * 数据分段
     */
    private final int index;
    /**
     * range 开始
     */
    private final long start;
    /**
     * range 结束
     */
    private final long end;

    private final ProxyRestTemplatePool proxyRestTemplatePool;

    public DownloadBigFileThread( ProxyRestTemplatePool proxyRestTemplatePool, HttpHeaders header, FileInfo fileInfo,
        int index,
        long start, long end) {
        this.header = header;
        this.fileInfo = fileInfo;
        this.index = index;
        this.start = start;
        this.end = end;
        this.proxyRestTemplatePool = proxyRestTemplatePool;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public FileInfo call() {
        int i = 0;
        while ( i < MAX_RETRY) {
            ProxyRestTemplateObject borrow = null;
            try {
                borrow = proxyRestTemplatePool.borrow();
                RestTemplate template = borrow.getRestTemplate();
                RequestCallback requestCallback = request -> {
                    HttpHeaders headers = request.getHeaders();
                    headers.putAll(header);
                    headers.set(HttpHeaders.RANGE, "bytes=" + start + "-" + end);
                };
                ResponseExtractor<Void> responseExtractor = response -> {
                    InputStream body = response.getBody();
                    fileInfo.saveBigFileCache(index, body);
                    return null;
                };
                header.set(HttpHeaders.RANGE, "bytes=" + start + "-" + end);
                template.execute(fileInfo.getRedictUrl(), HttpMethod.GET, requestCallback, responseExtractor);
                return fileInfo;
            }catch (ResourceAccessException e) {
                // 个别ip访问失败者，如果是代理，直接移除
                if (borrow != null && borrow.isProxy()) {
                    borrow.setDelete(true);
                }
            } catch (Exception e) {
                i = i+1;
                logger.error("下载文件产生未知异常,url:"+fileInfo.getRedictUrl()+",正在进行重试,当前次数:" + i ,e);
            }finally {
                if (borrow != null) {
                    proxyRestTemplatePool.returnObject(borrow);
                }
            }
        }
        fileInfo.setFail(false);
        return fileInfo;
    }

}
