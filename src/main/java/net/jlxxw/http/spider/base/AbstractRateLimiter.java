package net.jlxxw.http.spider.base;


import org.apache.hc.core5.http.HttpHost;

/**
 * 抽象限流器
 * @author lcy
 */
public interface AbstractRateLimiter {

    /**
     * 执行限流
     * @param httpHost 代理ip地址，在不能获取到代理地址的情况下，可能为null
     */
    void doLimiter(HttpHost httpHost);
}
