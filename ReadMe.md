# 说明
 本项目为工具集，请合理使用采集数据，切勿滥用造成不良影响
# 支持功能
- 通过自定义 http 代理,下载 html 文件
- 通过自定义 http 代理,高速下载自定义类型文件

## 使用方法
### 步骤一,创建代理生产者，并交给 spring 管理
```java
package net.jlxxw.http.spider.proxy;

import net.jlxxw.http.spider.base.AbstractProxyHostProducer;
import org.apache.http.HttpHost;
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
        // 从 ip 代理商获取数据
        return null;
    }

    /**
     * 当代理地址失效时，要被移除，通知代理生产者
     *
     * @param host 被移除的代理地址，注意，该对象可能为 null 
     */
    @Override
    public void destroy(HttpHost host) {
        // 检测ip能否复用，如果可以复用，可以继续使用
    }
}

```
### 步骤二，创建 cookie 存储器,并交给 Spring 管理（此操作可选）
`如果不需要存储 cookie 方法空实现就好了`
```java
package net.jlxxw.http.spider.proxy;

import java.util.Date;
import java.util.List;
import net.jlxxw.http.spider.base.AbstractCookieStore;
import org.apache.http.cookie.Cookie;
import org.springframework.stereotype.Component;

/**
 * @author chunyang.leng
 * @date 2023-09-01 15:24
 */
@Component
public class MyCookieStore extends AbstractCookieStore {
    /**
     * Adds an {@link Cookie}, replacing any existing equivalent cookies.
     * If the given cookie has already expired it will not be added, but existing
     * values will still be removed.
     *
     * @param cookie the {@link Cookie cookie} to be added
     */
    @Override public void addCookie(Cookie cookie) {
        
    }

    /**
     * Returns all cookies contained in this store.
     *
     * @return all cookies
     */
    @Override public List<Cookie> getCookies() {
        return null;
    }

    /**
     * Removes all of {@link Cookie}s in this store that have expired by
     * the specified {@link Date}.
     *
     * @param date
     * @return true if any cookies were purged.
     */
    @Override public boolean clearExpired(Date date) {
        return false;
    }

    /**
     * Clears all cookies.
     */
    @Override public void clear() {

    }
}
```
### 编写YML配置文件
```yaml
spider:
  # rest template 连接池专用配置，默认路由数量
  http:
    default-max-per-route: 200
    # rest template 连接池专用配置，最大连接数
    max-total: 400
    # 链接超时时间
    connect-timeout-millis: 100000
    # 请求超时时间
    connection-request-timeout-millis: 100000
    # http 并发下载文件配置
    concurrency:
      # 最大并发下载线程数
      max: 200
      # 单次下载最大等待时间，超时抛出 timeout 异常
      max-wait-millis: 300000
      # 最小并发数量
      min: 1
      # 等待队列容量
      queue-capacity: 200
  # 并发下载文件时，需要配置一些缓存文件信息
  file:
    # 缓存文件目录
    cache-file-path: ./cache
    # 允许下载的文件类型，只有添加到此处的类型才会允许下载
    media-types:
      - application/zip
      - application/x-rar
    # 大文件标识，超过此长度的文件标识为大文件，会启动并发下载策略,默认 15mb
    big-file-size: 15728640
    # 并发下载时，分片缓存文件大小，默认 15mb
    share-size: 15728640
  # 代理类相关配置信息
  proxy:
    pool:
      # 是否启用后进先出策略
      lifo: false
      # 代理池允许存在的空闲对象数量
      max-idle: 64
      # 代理池允许存在的最少对象数量
      min-idle: 1
      # 代理池允许存在的最大对象数量
      max-total: 64
      # 当无法获取池内对象时候，最大等待时间
      max-wait-millis: 15000
      # 从代理池获取对象时，是否检测对象可用性
      test-on-borrow: false
      # 将对象放回代理池时，是否检测对象可用性
      test-on-return: true
```

### 开始采集数据之路
```java
package net.jlxxw.http.spider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.jlxxw.http.spider.configuration.HttpSpiderAutoConfiguration;
import net.jlxxw.http.spider.file.FileInfo;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;

@SpringBootTest(classes = HttpSpiderAutoConfiguration.class)
public class HttpHttpSpiderAutoConfigurationTests {

    @Autowired
    private HttpSpider httpSpider;

    @Test
    void htmlTest() throws Exception {
        List<String> urls = new ArrayList<String>();
        urls.add("https://www.huaweicloud.com");
        urls.add("https://www.baidu.com/");
        urls.add("https://www.huaweicloud.com");
        urls.add("https://www.baidu.com/");
        for (String url : urls) {
            httpSpider.html(url, new HttpHeaders(), (html) -> {
                Assertions.assertNotNull(html, "获取的html数据，不应该为空");
            });
        }
    }

    @Test
    void download() throws IOException {
        List<String> urls = new ArrayList<String>();
        urls.add("https://download.jetbrains.com.cn/idea/ideaIU-2023.2.1-aarch64.dmg");
        urls.add("https://www.winrar.com.cn/download/winrar-x64-611scp.exe");
        for (String url : urls) {
            FileInfo info = httpSpider.downloadFile(url, new HttpHeaders());
            boolean fail = info.isFail();
            Assertions.assertFalse(fail, "下载不应该失败");

            String name = info.getFileName();
            Assertions.assertTrue(StringUtils.isNotBlank(name), "获取的数据名称不可能为空");
            long length = info.getLength();

            Assertions.assertTrue(length > 0, "文件长度不可能小于 0");

            byte[] data = info.getData();
            Assertions.assertTrue(data.length > 0, "文件长度不可能小于 0");

        }
    }
}
```

