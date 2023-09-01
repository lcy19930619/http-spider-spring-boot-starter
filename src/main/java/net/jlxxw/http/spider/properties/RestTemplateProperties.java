package net.jlxxw.http.spider.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 爬虫采集器配置
 * @author chunyang.leng
 * @date 2023-09-01 12:53
 */
@ConfigurationProperties("spider.http")
@Configuration
public class RestTemplateProperties {

    /**
     * 默认连接数
     */
    private int defaultMaxPerRoute = 200;

    /**
     * 最大连接数
     */
    private int maxTotal = 2 * defaultMaxPerRoute;

    /**
     * socket 链接超时时间
     */
    private int connectTimeoutMillis = 10000;

    /**
     * 请求链接超时时间
     */
    private int connectionRequestTimeoutMillis = 10000;



    public int getDefaultMaxPerRoute() {
        return defaultMaxPerRoute;
    }

    public void setDefaultMaxPerRoute(int defaultMaxPerRoute) {
        this.defaultMaxPerRoute = defaultMaxPerRoute;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getConnectionRequestTimeoutMillis() {
        return connectionRequestTimeoutMillis;
    }

    public void setConnectionRequestTimeoutMillis(int connectionRequestTimeoutMillis) {
        this.connectionRequestTimeoutMillis = connectionRequestTimeoutMillis;
    }

}
