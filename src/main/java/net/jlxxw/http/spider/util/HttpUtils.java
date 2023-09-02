package net.jlxxw.http.spider.util;

import net.jlxxw.http.spider.base.AbstractRateLimiter;
import net.jlxxw.http.spider.proxy.ProxyRestTemplateObject;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpUtils {
    private static AbstractRateLimiter abstractRateLimiter;

    public HttpUtils(@Autowired(required = false) AbstractRateLimiter abstractRateLimiter) {
        HttpUtils.abstractRateLimiter = abstractRateLimiter;
    }

    /**
     * http get 请求
     * @param object 具有代理能力的 rest template 对象
     * @param url 请求的地址
     * @param method method
     * @param responseType 应答数据类型
     * @return 请求结果
     * @param <T> 数据类型
     */
    public static <T> ResponseEntity<T> exchange(ProxyRestTemplateObject object, String url, HttpMethod method,
                                                 @Nullable HttpEntity<?> requestEntity, Class<T> responseType) {
        HttpHost host = object.getHost();
        if (abstractRateLimiter != null) {
            abstractRateLimiter.doLimiter(host);
        }
        RestTemplate restTemplate = object.getRestTemplate();
        return restTemplate.exchange(url, method, requestEntity, responseType);
    }

    public static <T> T  execute(ProxyRestTemplateObject object, String url, HttpMethod method, @Nullable RequestCallback requestCallback,
                         @Nullable ResponseExtractor<T> responseExtractor){
        HttpHost host = object.getHost();
        if (abstractRateLimiter != null) {
            abstractRateLimiter.doLimiter(host);
        }
        RestTemplate restTemplate = object.getRestTemplate();
        return restTemplate.execute(url, method, requestCallback, responseExtractor);

    }
}
