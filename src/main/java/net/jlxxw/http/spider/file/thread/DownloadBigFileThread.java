package net.jlxxw.http.spider.file.thread;

import java.io.InputStream;
import net.jlxxw.http.spider.file.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RequestCallback;
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
     * 下载器
     */
    private final RestTemplate restTemplate;
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

    public DownloadBigFileThread(RestTemplate restTemplate, HttpHeaders header, FileInfo fileInfo,
        int index,
        long start, long end) {
        this.restTemplate = restTemplate;
        this.header = header;
        this.fileInfo = fileInfo;
        this.index = index;
        this.start = start;
        this.end = end;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public void download() throws Exception {

        RequestCallback requestCallback = request -> {
            HttpHeaders headers = request.getHeaders();
            headers.putAll(header);
            headers.set(HttpHeaders.RANGE, "bytes=" + start + "-" + end);
        };
        ResponseExtractor<Void> responseExtractor = response -> {
            InputStream body = response.getBody();
            fileInfo.saveBigFile(index, body);
            return null;
        };

        header.set(HttpHeaders.RANGE, "bytes=" + start + "-" + end);
        restTemplate.execute(fileInfo.getRedictUrl(), HttpMethod.GET, requestCallback, responseExtractor);
    }
}
