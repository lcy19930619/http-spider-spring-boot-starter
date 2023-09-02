package net.jlxxw.http.spider.proxy;

import java.util.function.Consumer;
import net.jlxxw.http.spider.properties.ProxyPoolProperties;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * @author chunyang.leng
 * @date 2023-08-23 13:46
 */
public class ProxyRestTemplatePool extends GenericObjectPool<ProxyRestTemplateObject> {

    public ProxyRestTemplatePool(ProxyRestTemplateFactory proxyRestTemplateFactory, ProxyPoolProperties proxyPoolProperties) {
        super(proxyRestTemplateFactory);
        setMinIdle(proxyPoolProperties.getMinIdle());
        setMaxIdle(proxyPoolProperties.getMaxIdle());
        setMaxTotal(proxyPoolProperties.getMaxTotal());
        setLifo(proxyPoolProperties.isLifo());
        setTestOnBorrow(proxyPoolProperties.isTestOnBorrow());
        setTestOnReturn(proxyPoolProperties.isTestOnReturn());
        setMaxWaitMillis(proxyPoolProperties.getMaxWaitMillis());
    }

    /**
     * 自旋获取对象
     * @return
     */
    public ProxyRestTemplateObject borrow(){
        try {
            return borrowObject();
        }catch(Exception e) {
            return borrow();
        }
    }
}
