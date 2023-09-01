package net.jlxxw.http.spider.file.thread;

import net.jlxxw.http.spider.file.FileInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

/**
 * 抽象文件下载线程
 *
 * @author chunyang.leng
 * @date 2023-08-31 14:51
 */
public abstract class AbstractDownloadFileThread {

    /**
     * 创建小文件下载线程
     *
     * @param restTemplate 下载工具
     * @param header       请求头
     * @param fileInfo     文件信息
     * @return
     */
    public static AbstractDownloadFileThread littleFileThread(RestTemplate restTemplate, HttpHeaders header,
        FileInfo fileInfo) {
        return new DownloadLittleFileThread(restTemplate, header, fileInfo);
    }

    /**
     * 创建下载大文件线程
     *
     * @param restTemplate 下载工具
     * @param header       请求头
     * @param fileInfo     文件信息
     * @param index        分片索引
     * @param start        开始索引
     * @param end          结束索引
     */
    public static AbstractDownloadFileThread bigFileThread(RestTemplate restTemplate, HttpHeaders header,
        FileInfo fileInfo,
        int index,
        long start,
        long end) {
        return new DownloadBigFileThread(restTemplate, header, fileInfo, index, start, end);
    }

    /**
     * 执行下载
     *
     * @return 下载后的文件信息
     * @throws Exception 下载过程中出现了未知的异常
     */
    public abstract void download() throws Exception;
}
