package net.jlxxw.http.spider.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 代理池配置
 * @author chunyang.leng
 * @date 2023-09-01 12:33
 */
@ConfigurationProperties("spider.proxy.pool")
@Configuration
public class ProxyPoolProperties {

    /**
     * 代理池最小空闲
     */
    private int minIdle = 1;

    /**
     * 代理池最大空闲
     */
    private int maxIdle = 8;
    /**
     * 代理池最大数量
     */
    private int maxTotal = 16;
    /**
     * 后进先出
     */
    private boolean lifo = false;
    /**
     * 探测代理是否可用
     */
    private boolean testOnBorrow = false;

    /**
     * 探测代理是否可用
     */
    private boolean testOnReturn = true;
    /**
     * 最大等待时间
     */
    private long maxWaitMillis = 15000;

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public boolean isLifo() {
        return lifo;
    }

    public void setLifo(boolean lifo) {
        this.lifo = lifo;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }
}
