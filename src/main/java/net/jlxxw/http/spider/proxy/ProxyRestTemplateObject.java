package net.jlxxw.http.spider.proxy;

import net.jlxxw.http.spider.base.AbstractProxyHostProducer;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.client.RestTemplate;

/**
 * @author chunyang.leng
 * @date 2023-08-23 14:03
 */
public class ProxyRestTemplateObject extends DefaultPooledObject<RestTemplate> implements DisposableBean {

    private final RestTemplate restTemplate;

    /**
     * 是否需要删除
     */
    private boolean delete = false;

    /**
     * 是否是代理模式
     */
    private final boolean proxy;

    /**
     * 生产代理地址的实现类
     */
    private final AbstractProxyHostProducer abstractProxyHostProducer;

    /**
     * 代理地址
     */
    private final HttpHost host;
    /**
     * Create a new instance that wraps the provided object so that the pool can
     * track the state of the pooled object.
     *
     * @param object The object to wrap
     */
    public ProxyRestTemplateObject(RestTemplate object, AbstractProxyHostProducer abstractProxyHostProducer,
        HttpHost host) {
        super(object);
        this.restTemplate = object;
        this.proxy = host != null;
        this.abstractProxyHostProducer = abstractProxyHostProducer;
        this.host = host;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public boolean validate() {
        return !delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public boolean isDelete() {
        return delete;
    }

    public boolean isProxy() {
        return proxy;
    }

    @Override
    public void destroy() throws Exception {
        if (abstractProxyHostProducer != null) {
            abstractProxyHostProducer.destroy(host);
        }
    }
}
