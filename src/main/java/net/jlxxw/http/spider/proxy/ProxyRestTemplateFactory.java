package net.jlxxw.http.spider.proxy;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author chunyang.leng
 * @date 2023-08-23 13:14
 */
public class ProxyRestTemplateFactory extends BasePooledObjectFactory<ProxyRestTemplateObject> {
    private static final Logger logger = LoggerFactory.getLogger(ProxyRestTemplateFactory.class);

    private BeanFactory beanFactory;

    public ProxyRestTemplateFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Creates an object instance, to be wrapped in a {@link PooledObject}.
     * <p>This method <strong>must</strong> support concurrent, multi-threaded
     * activation.</p>
     *
     * @return an instance to be served by the pool
     * @throws Exception if there is a problem creating a new instance,
     *                   this will be propagated to the code requesting an object.
     */
    @Override
    public ProxyRestTemplateObject create() throws Exception {
        return beanFactory.getBean("spiderProxyRestTemplateObject", ProxyRestTemplateObject.class);
    }

    /**
     * Wrap the provided instance with an implementation of
     * {@link PooledObject}.
     *
     * @param obj the instance to wrap
     * @return The provided instance, wrapped by a {@link PooledObject}
     */
    @Override
    public PooledObject<ProxyRestTemplateObject> wrap(ProxyRestTemplateObject obj) {
        return new DefaultPooledObject<>(obj);
    }

    /**
     * This implementation always returns {@code true}.
     *
     * @param p ignored
     * @return {@code true}
     */
    @Override
    public boolean validateObject(PooledObject<ProxyRestTemplateObject> p) {
        ProxyRestTemplateObject object = p.getObject();
        boolean proxy = object.isProxy();
        // 默认标识对象可用
        boolean validate = true;
        if (proxy) {
            // 代理对象通过 pool 自己掌控对象可用性
            validate =  object.validate();
        }else {
            // 默认对象使用生存时间进行检测对象可用性，默认生存时间 5 分钟
            long createTime = object.getCreateTime();
            long currentTimeMillis = System.currentTimeMillis();
            // 对象生存周期5分钟
            validate =  currentTimeMillis - createTime < 5 * 60 * 1000;
        }
        if (!validate) {
            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory)beanFactory;
            defaultListableBeanFactory.destroyBean(object);
        }
       return validate;
    }
}
