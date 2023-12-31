package net.jlxxw.http.spider.file.thread;

import net.jlxxw.http.spider.file.FileInfo;
import net.jlxxw.http.spider.proxy.ProxyRestTemplateObject;
import net.jlxxw.http.spider.proxy.ProxyRestTemplatePool;
import net.jlxxw.http.spider.util.HttpUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 下载小文件线程
 * @author chunyang.leng
 * @date 2023-08-31 14:44
 */
class DownloadLittleFileThread extends AbstractDownloadFileThread {
    private static final Logger logger = LoggerFactory.getLogger(DownloadLittleFileThread.class);
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

    private final ProxyRestTemplatePool proxyRestTemplatePool;

    public DownloadLittleFileThread( ProxyRestTemplatePool proxyRestTemplatePool, HttpHeaders header,
        FileInfo fileInfo) {
        this.header = header;
        this.fileInfo = fileInfo;
        this.proxyRestTemplatePool = proxyRestTemplatePool;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public FileInfo call()  {
        int i = 0;
        while ( i < MAX_RETRY) {
            ProxyRestTemplateObject borrow = null;
            try {
                borrow = proxyRestTemplatePool.borrow();
                ResponseEntity<byte[]> rsp = HttpUtils.exchange(borrow,fileInfo.getRedirectUrl(), HttpMethod.GET, new HttpEntity<>(header), byte[].class);
                byte[] body = rsp.getBody();
                fileInfo.saveLittleFile(body);
                return fileInfo;
            } catch (HttpHostConnectException e) {
                if ( borrow.isProxy()) {
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
                HttpStatus statusCode = e.getStatusCode();
                if (statusCode.value() == 403) {
                    // 个别ip访问失败者，如果是代理，直接移除
                    if (borrow != null && borrow.isProxy()) {
                        borrow.setDelete(true);
                    }
                }else {
                    throw e;
                }
            }  catch (Exception e) {
                i = i+1;
                logger.error("下载文件产生未知异常,url:"+fileInfo.getRedirectUrl()+",正在进行重试,当前次数:" + i ,e);
            }finally {
                if (borrow != null) {
                    proxyRestTemplatePool.returnObject(borrow);
                }
            }
        }
        fileInfo.setFail(true);
        return fileInfo;
    }
}
