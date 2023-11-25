package net.jlxxw.http.spider.file;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.jlxxw.http.spider.file.thread.AbstractDownloadFileThread;
import net.jlxxw.http.spider.interceptor.FileInterceptor;
import net.jlxxw.http.spider.properties.FileProperties;
import net.jlxxw.http.spider.properties.HttpConcurrencyPoolProperties;
import net.jlxxw.http.spider.proxy.ProxyRestTemplatePool;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

/**
 *
 */
public class DownloadFileTools {
    private static final Logger log = LoggerFactory.getLogger(DownloadFileTools.class);

    /**
     * 最大允许使用的线程数量
     */
    private int maxPoolSize;

    private FileInterceptor fileInterceptor;
    /**
     * 并发下载线程池
     */
    private final ThreadPoolTaskExecutor httpDownloadExecutor;


    private final FileProperties fileProperties;

    private final HttpConcurrencyPoolProperties httpConcurrencyPoolProperties;

    private final ProxyRestTemplatePool proxyRestTemplatePool;
    /**
     * 创建一个默认的下载文件服务
     *
     * @param httpDownloadExecutor
     * @param beanFactory
     */
    public DownloadFileTools(ThreadPoolTaskExecutor httpDownloadExecutor, BeanFactory beanFactory,
        FileProperties fileProperties, HttpConcurrencyPoolProperties httpConcurrencyPoolProperties,ProxyRestTemplatePool proxyRestTemplatePool,FileInterceptor fileInterceptor) {
        this.fileInterceptor = fileInterceptor;
        this.httpDownloadExecutor = httpDownloadExecutor;
        this.fileProperties = fileProperties;
        this.maxPoolSize = httpConcurrencyPoolProperties.getMax();
        this.httpConcurrencyPoolProperties = httpConcurrencyPoolProperties;
        this.proxyRestTemplatePool = proxyRestTemplatePool;
    }

    /**
     * 下载文件并返回文件基本信息
     *
     * @param url        下载连接
     * @param httpHeader 要增加的header，如果header存在数据，则进行覆盖
     * @return fileName
     */
    public FileInfo download(String url, HttpHeaders httpHeader) throws Exception {

        //调用head方法,只获取头信息,拿到文件大小
        HttpHeaders responseHeader = readLength(url, httpHeader);

        FileInfo info = createFileInfo(responseHeader);

        String redirectUrl = info.getRedirectUrl();
        if (StringUtils.isBlank(redirectUrl)){
            info.setRedirectUrl(url);
        }
        long contentLength = info.getLength();

        boolean allowDownload = fileInterceptor.allowDownload(contentLength);
        if (!allowDownload) {
            info.setFail(true);
            return info;
        }
        //开启线程
        long threadNum = threadNum(contentLength);

        int thread = (int) threadNum;

        boolean bigFile = info.isBigFile();

        List<Future<FileInfo>> downLoadFileThreads = new ArrayList<Future<FileInfo>>(thread);
        //每个线程下载的大小
        long tempLength = fileProperties.getShareSize();
        // 分片数据的开始位置
        long start;
        // 分片数据的结束位置
        long end;
        // 分片数据的索引位置
        int index = 0;
        // 被分配的数据大小
        int totalSize = 0;
        for (int i = 0; i < threadNum && totalSize < contentLength; ++i) {
            // 累加
            start = i * tempLength;
            end = start + tempLength - 1;
            totalSize += tempLength;
            AbstractDownloadFileThread downloadThread;
            if (bigFile) {
                downloadThread = AbstractDownloadFileThread.bigFileThread(proxyRestTemplatePool, httpHeader, info, index, start, end);
            } else {
                downloadThread = AbstractDownloadFileThread.littleFileThread(proxyRestTemplatePool, httpHeader, info);
            }
            Future<FileInfo> submit = httpDownloadExecutor.submit(downloadThread);
            downLoadFileThreads.add(submit);
            index += 1;
        }

        for (Future<FileInfo> future : downLoadFileThreads) {
            try {
                future.get(httpConcurrencyPoolProperties.getMaxWaitMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                for (Future<FileInfo> fileThread : downLoadFileThreads) {
                    if (fileThread.isCancelled()) {
                        continue;
                    }
                    // 产生异常，则取消其余部分执行
                    fileThread.cancel(true);
                }
                log.error("文件下载产生未知异常",e);
            }
        }

        return info;
    }

    private HttpHeaders readLength(String url, HttpHeaders httpHeader) {

        RequestConfig requestConfig = RequestConfig
            .custom()
            // 从连接池中获取连接的超时时间，超过该时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException: Timeout waiting for connection from pool
            .setConnectionRequestTimeout(10000,TimeUnit.MILLISECONDS)
            // 禁止重定向，方便读取 location
            .setRedirectsEnabled(false)
            .build();
        HttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .build();
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(client);

        RestTemplate restTemplate = new RestTemplate(httpComponentsClientHttpRequestFactory);

        HttpEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.HEAD, new HttpEntity<>(null, httpHeader),
            byte[].class);
        HttpHeaders responseHeaders = response.getHeaders();

        URI location = responseHeaders.getLocation();
        if (Objects.nonNull(location)) {
            httpHeader.set("target-url", location.toString());
            return readLength(location.toString(), httpHeader);
        }
        HttpHeaders res = new HttpHeaders();
        res.putAll(responseHeaders);
        res.set("target-url", url);
        return res;
    }

    /**
     * 计算要启用的线程数量
     *
     * @param length
     * @return
     */
    private long threadNum(long length) {
        long l = length % fileProperties.getShareSize();
        if (l == 0) {
            return length / fileProperties.getShareSize();
        }
        return (length / fileProperties.getShareSize()) + 1;
    }

    /**
     * 判断是否为大文件
     *
     * @param length 文件总长度
     * @return 是否为大文件
     */
    private boolean isBigFile(long length) {
        return length > fileProperties.getBigFileSize();
    }

    /**
     * 创建一个文件信息的基础部分
     *
     * @param headers head 探测的http头
     * @return 基本文件信息，包括 length ，name,
     */
    private FileInfo createFileInfo(HttpHeaders headers) throws UnsupportedEncodingException {
        String fileName = "";
        String redictUrl = null;

        List<String> list = headers.get("Content-Disposition");
        if (CollectionUtils.isEmpty(list)) {
            // 尝试检测文件是否为重定向，如果为重定向，则使用重定向url解析文静名
            List<String> targetUrls = headers.get("target-url");

            if (!CollectionUtils.isEmpty(targetUrls)) {
                redictUrl = targetUrls.get(0);
            }
            String s = StringUtils.substringBeforeLast(redictUrl, "?");

            fileName = StringUtils.substringAfterLast(s, "/");
            fileName = URLDecoder.decode(fileName, String.valueOf(StandardCharsets.UTF_8));

        } else {
            String disposition = list.get(0);
            fileName = disposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
            fileName = URLDecoder.decode(fileName, String.valueOf(StandardCharsets.ISO_8859_1));
        }
        long contentLength = headers.getContentLength();




        boolean bigFile = isBigFile(contentLength);
        int share = share(contentLength);
        FileInfo fileInfo;
        if (bigFile) {
            String cacheFilePath = fileProperties.getCacheFilePath();
            fileInfo = new FileInfo(fileName, contentLength, share,cacheFilePath + "/" + UUID.randomUUID(), true);
        } else {
            fileInfo = new FileInfo(fileName, contentLength);
        }
        fileInfo.setRedirectUrl(redictUrl);
        return fileInfo;
    }

    /**
     * 获取数据分片数量
     *
     * @param contentLength 数据总长度
     * @return 分片数量
     */
    private int share(long contentLength) {
        if (contentLength % fileProperties.getShareSize() == 0) {
            return (int) (contentLength / fileProperties.getShareSize());
        }
        return ((int) (contentLength / fileProperties.getShareSize())) + 1;
    }

}

