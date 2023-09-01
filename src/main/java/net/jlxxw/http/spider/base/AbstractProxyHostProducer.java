package net.jlxxw.http.spider.base;

import org.apache.http.HttpHost;

/**
 * http 代理生产者,需要使用者自己实现此接口，提供代理获取服务
 * @author chunyang.leng
 * @date 2023-09-01 12:48
 */
public interface AbstractProxyHostProducer {

    /**
     * 创建一个代理地址
     * @return 如果无法创建代理地址，可以返回null，使用当前服务器进行网络请求
     */
    HttpHost producer() ;

    /**
     * 当代理地址失效时，要被移除，通知代理生产者
     *
     * @param host 被移除的代理地址，注意，该对象可能为 null
     */
    void destroy(HttpHost host);
}
