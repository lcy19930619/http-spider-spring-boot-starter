package net.jlxxw.http.spider.proxy;

import net.jlxxw.http.spider.base.AbstractProxyHostProducer;

import org.apache.hc.core5.http.HttpHost;
import org.springframework.stereotype.Component;

/**
 * @author chunyang.leng
 * @date 2023-09-01 15:22
 */
@Component
public class MyProxy implements AbstractProxyHostProducer {
    /**
     * 创建一个代理地址
     *
     * @return 如果无法创建代理地址，可以返回null，使用当前服务器进行网络请求
     */
    @Override public HttpHost producer() {
        return null;
    }

    /**
     * 当代理地址失效时，要被移除，通知代理生产者
     *
     * @param host 被移除的代理地址，注意，该对象可能为 null
     */
    @Override
    public void destroy(HttpHost host) {

    }
}
