package net.jlxxw.http.spider.proxy;

import com.google.common.util.concurrent.RateLimiter;
import net.jlxxw.http.spider.base.AbstractRateLimiter;

import org.apache.hc.core5.http.HttpHost;
import org.springframework.stereotype.Component;

@Component
public class MyLimiter implements AbstractRateLimiter {
    RateLimiter rateLimiter = RateLimiter.create(10d);
    @Override
    public void doLimiter(HttpHost httpHost) {
        if (httpHost == null) {
            System.out.println("获取的代理地址是空的，当前链接使用的是本机网卡");
           // 本机网络访问限速 qps = 10
            rateLimiter.acquire();
            return;
        }
        // 代理服务访问，限速可选，亦可以根据 host 针对性限速，比如redis 的 zset 限流方案等
    }
}
