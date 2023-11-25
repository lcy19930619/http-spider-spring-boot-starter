package net.jlxxw.http.spider.interceptor;

/**
 * 文件拦截器
 */
public interface FileInterceptor {
    /**
     * 是否允许下载文件
     * @param fileSize
     * @return
     */
    boolean allowDownload(long fileSize);
}
