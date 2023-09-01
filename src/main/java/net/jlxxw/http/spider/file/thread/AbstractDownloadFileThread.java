package net.jlxxw.http.spider.file.thread;

import java.util.concurrent.Callable;
import net.jlxxw.http.spider.file.FileInfo;
import net.jlxxw.http.spider.proxy.ProxyRestTemplatePool;
import org.springframework.http.HttpHeaders;

/**
 * 抽象文件下载线程
 *
 * @author chunyang.leng
 * @date 2023-08-31 14:51
 */
public abstract class AbstractDownloadFileThread implements Callable<FileInfo> {

    /**
     * 创建小文件下载线程
     *
     * @param header       请求头
     * @param fileInfo     文件信息
     * @return
     */
    public static AbstractDownloadFileThread littleFileThread( ProxyRestTemplatePool proxyRestTemplatePool, HttpHeaders header,
        FileInfo fileInfo) {
        return new DownloadLittleFileThread(proxyRestTemplatePool, header, fileInfo);
    }

    /**
     * 创建下载大文件线程
     *
     * @param header       请求头
     * @param fileInfo     文件信息
     * @param index        分片索引
     * @param start        开始索引
     * @param end          结束索引
     */
    public static AbstractDownloadFileThread bigFileThread( ProxyRestTemplatePool proxyRestTemplatePool, HttpHeaders header,
        FileInfo fileInfo,
        int index,
        long start,
        long end) {
        return new DownloadBigFileThread(proxyRestTemplatePool, header, fileInfo, index, start, end);
    }

}
