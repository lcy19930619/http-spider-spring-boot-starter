package net.jlxxw.http.spider.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.jlxxw.http.spider.HttpSpider;
import net.jlxxw.http.spider.base.AbstractCookieStore;
import net.jlxxw.http.spider.base.AbstractProxyHostProducer;
import net.jlxxw.http.spider.adapter.HttpSpiderAdapter;
import net.jlxxw.http.spider.file.DownloadFileTools;
import net.jlxxw.http.spider.interceptor.DefaultFileInterceptor;
import net.jlxxw.http.spider.interceptor.FileInterceptor;
import net.jlxxw.http.spider.properties.FileProperties;
import net.jlxxw.http.spider.properties.HttpConcurrencyPoolProperties;
import net.jlxxw.http.spider.properties.ProxyPoolProperties;
import net.jlxxw.http.spider.properties.RestTemplateProperties;
import net.jlxxw.http.spider.proxy.ProxyRestTemplateFactory;
import net.jlxxw.http.spider.proxy.ProxyRestTemplateObject;
import net.jlxxw.http.spider.proxy.ProxyRestTemplatePool;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

/**
 * http 爬虫代理自动装配
 *
 * @author chunyang.leng
 * @date 2023-09-01 12:48
 */
@ComponentScan("net.jlxxw.http.spider")
public class HttpSpiderAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(HttpSpiderAutoConfiguration.class);

    @Bean
    public ProxyRestTemplateFactory proxyRestTemplateFactory(BeanFactory beanFactory) {
        logger.info("HttpSpider ProxyRestTemplateFactory created");
        return new ProxyRestTemplateFactory(beanFactory);
    }

    @Bean
    public ProxyRestTemplatePool proxyRestTemplatePool(ProxyRestTemplateFactory proxyRestTemplateFactory,
        ProxyPoolProperties proxyPoolProperties) {
        logger.info("HttpSpider ProxyRestTemplatePool created");
        return new ProxyRestTemplatePool(proxyRestTemplateFactory, proxyPoolProperties);
    }

    @Bean
    @Scope("prototype")
    public ProxyRestTemplateObject spiderProxyRestTemplateObject(RestTemplateProperties restTemplateProperties,
        @Autowired(required = false) AbstractProxyHostProducer abstractProxyHostProducer,
        @Autowired(required = false) AbstractCookieStore abstractCookieStore) {

        HttpHost proxyHost = null;
        if (abstractProxyHostProducer != null) {
            proxyHost = abstractProxyHostProducer.producer();
            if (proxyHost == null) {
                logger.warn("未能获取到代理服务器地址");
            }
        }
        boolean proxy = Objects.nonNull(proxyHost);

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", SSLConnectionSocketFactory.getSocketFactory())
            .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        //设置整个连接池最大连接数
        connectionManager.setMaxTotal(restTemplateProperties.getMaxTotal());
        //路由是对maxTotal的细分
        connectionManager.setDefaultMaxPerRoute(restTemplateProperties.getDefaultMaxPerRoute());
        RequestConfig requestConfig = RequestConfig
            .custom()
            //从连接池中获取连接的超时时间，超过该时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException: Timeout waiting for connection from pool
            //从连接池中获取连接的超时时间，超过该时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException: Timeout waiting for connection from pool
            .setConnectionRequestTimeout(restTemplateProperties.getConnectionRequestTimeoutMillis(), TimeUnit.MILLISECONDS)
            .setConnectTimeout(restTemplateProperties.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)
            .setProxy(proxyHost)
            .build();

        CookieStore cookieStore = Objects.isNull(abstractCookieStore) ? new BasicCookieStore() : abstractCookieStore;
        HttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .setDefaultCookieStore(cookieStore)
            .build();

        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(client));

        logger.info("created ProxyRestTemplateObject ,proxy model:{}", proxy);

        return new ProxyRestTemplateObject(restTemplate, abstractProxyHostProducer,proxyHost);
    }

    @Bean
    public ThreadPoolTaskExecutor httpConcurrencyDownloadExecutor(
        HttpConcurrencyPoolProperties httpConcurrencyPoolProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心池大小
        executor.setCorePoolSize(httpConcurrencyPoolProperties.getMin());
        // 最大线程数
        executor.setMaxPoolSize(httpConcurrencyPoolProperties.getMax());
        // 队列长度
        executor.setQueueCapacity(httpConcurrencyPoolProperties.getQueueCapacity());
        // 线程空闲时间
        executor.setKeepAliveSeconds(1000);
        // 线程前缀名称
        executor.setThreadNamePrefix("http-concurrency-pool-");
        // 配置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean
    public DownloadFileTools downloadFileTools(ThreadPoolTaskExecutor httpConcurrencyDownloadExecutor,
        BeanFactory beanFactory,
        FileProperties fileProperties, HttpConcurrencyPoolProperties httpConcurrencyPoolProperties,ProxyRestTemplatePool proxyRestTemplatePool) {
        return new DownloadFileTools(httpConcurrencyDownloadExecutor, beanFactory, fileProperties, httpConcurrencyPoolProperties,proxyRestTemplatePool);
    }

    @Bean
    public HttpSpider httpSpider(ProxyRestTemplatePool proxyRestTemplatePool, DownloadFileTools downloadFileTools) {
        return new HttpSpiderAdapter(proxyRestTemplatePool,downloadFileTools);
    }

    @Bean
    public ByteArrayHttpMessageConverter spiderDownloadFileHttpMessageConverter(FileProperties fileProperties){
        ByteArrayHttpMessageConverter byteArrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        List<MediaType> list = new ArrayList<MediaType>();
        list.add(MediaType.APPLICATION_OCTET_STREAM);

        Set<String> types = fileProperties.getMediaTypes();
        for (String type : types) {
            list.add(MediaType.valueOf(type));
        }
        byteArrayHttpMessageConverter.setSupportedMediaTypes(list);
        return byteArrayHttpMessageConverter;
    }

    @Bean
    @ConditionalOnMissingBean(FileInterceptor.class)
    public FileInterceptor fileInterceptor() {
        logger.info("启用默认下载拦截器，允许全部文件下载");
        return new DefaultFileInterceptor();
    }

}
