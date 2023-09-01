package net.jlxxw.http.spider.proxy;

import java.util.function.Consumer;
import net.jlxxw.http.spider.properties.ProxyPoolProperties;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.http.conn.HttpHostConnectException;
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
     * 执行相关请求
     * @throws Exception
     */
    public void doExecute(Consumer<ProxyRestTemplateObject> consumer) throws Exception {
        ProxyRestTemplateObject proxyRestTemplateObject = null;
        try {
            proxyRestTemplateObject = borrowObject();
            consumer.accept(proxyRestTemplateObject);
        } catch (HttpHostConnectException | ResourceAccessException e) {
            // 个别ip访问失败者，如果是代理，直接移除
            if (proxyRestTemplateObject != null && proxyRestTemplateObject.isProxy()) {
                proxyRestTemplateObject.setDelete(true);
            }
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (proxyRestTemplateObject != null) {
                returnObject(proxyRestTemplateObject);
            }
        }
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
