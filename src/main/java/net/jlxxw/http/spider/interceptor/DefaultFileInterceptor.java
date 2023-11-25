package net.jlxxw.http.spider.interceptor;

/**
 * 文件拦截器
 */
public class DefaultFileInterceptor implements FileInterceptor{
    @Override
    public boolean allowDownload(long fileSize) {
        return true;
    }
}
