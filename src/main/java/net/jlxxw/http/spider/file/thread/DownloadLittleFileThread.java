package net.jlxxw.http.spider.file.thread;

import net.jlxxw.http.spider.file.FileInfo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * 下载小文件线程
 * @author chunyang.leng
 * @date 2023-08-31 14:44
 */
class DownloadLittleFileThread extends AbstractDownloadFileThread {
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

    public DownloadLittleFileThread(RestTemplate restTemplate, HttpHeaders header,
        FileInfo fileInfo) {
        this.restTemplate = restTemplate;
        this.header = header;
        this.fileInfo = fileInfo;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public void download() throws Exception {

        ResponseEntity<byte[]> rsp = restTemplate.exchange(fileInfo.getRedictUrl(), HttpMethod.GET, new HttpEntity<>(header), byte[].class);
        byte[] body = rsp.getBody();
        fileInfo.saveLittleFile(body);
    }
}
