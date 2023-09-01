package net.jlxxw.http.spider.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * http 并发池配置
 * @author chunyang.leng
 * @date 2023-09-01 12:45
 */
@ConfigurationProperties("spider.http.concurrency")
@Configuration
public class HttpConcurrencyPoolProperties {

    /**
     * 并发池最小空闲
     */
    private int min = 1;
    /**
     * 并发池最大数量
     */
    private int max = 200;
    /**
     * 队列容量
     */
    private int queueCapacity = 200;
    /**
     * 下载数据时候，最大等待时间
     */
    private long maxWaitMillis = 150000;

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }
}
