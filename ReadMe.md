# 说明
 本项目为工具集，请合理使用采集数据，切勿滥用造成不良影响
# 支持功能
- 通过自定义 http 代理,下载 html 文件
- 通过自定义 http 代理,高速下载自定义类型文件

# 工作原理
- 代理原理
  - 核心请求器 apache client ,速度较快，性能较好，配合 Spring RestTemplate 效果很不错
  - 请求 html url，获取原生html文件，此方式适用于目标网站为模板引擎网站，例如，JSP、velocity 、freemarker等
  - 发起 http 请求，检测代理池是否存在代理对象
  - 如不存在代理对象，则使用 `net.jlxxw.http.spider.configuration.HttpSpiderAutoConfiguration.spiderProxyRestTemplateObject` 方法，创建一个代理对象，该对象为池化对象，且为原型模式，并且该对象被 spring 管理
  - spiderProxyRestTemplateObject 初始化步骤会调用 `net.jlxxw.http.spider.base.AbstractProxyHostProducer.producer` 方法，获取代理链接地址，如果无法获取，则使用本机之间发起请求
  - 如果 配置了 `test-on-borrow` 或者 `test-on-return`  会对对象进行检测，如果验证失败，调用 BeanFactory 销毁方法，销毁代理对象，会触发  `net.jlxxw.http.spider.base.AbstractProxyHostProducer.destroy` 通知代理池代理链接地址已经失效
  - 网络请求： 本地发起 http 调用 ---> 检测是否存在代理服务器 ---> 与代理服务器建立链接 ---> 将请求发生到代理服务，由代理服务转发至目标服务器
  - 代理对象池化配置 `net.jlxxw.http.spider.properties.ProxyPoolProperties`
- 下载文件部分
  - 如要下载文件，需要先添加 `media-type` 相关配置，这样 `ByteArrayHttpMessageConverter` 才能正确识别转换成 `byte[]`
  - 根据配置的 `big-file-size` 判断文件是否为大文件
  - 如果为小文件，则直接进行 `http` 下载，文件数据存储在内存中，需要及时取出并处理
  - 如果为大文件，则会根据 `share-size` 分段下载文件，此时下载文件的每个 http client 都是可以使用不同的代理服务器，部分文件服务器针对单个ip地址由限速，使用次方法可以翻倍提升下载速度（前提是使用不限速的代理服务器），达到高速下载目的
- 大文件下载原理
  - 采用分段下载思想，先用 `http head` 请求，获取文件长度
  - 如果目标服务器有重定向相关能力，会自旋转重定向跟随，直到找到真正下载地址
  - 根据目标文件长度，以及配置的分片大小，开启多线程分段下载，并发线程池配置参考 `net.jlxxw.http.spider.properties.HttpConcurrencyPoolProperties`
  - 分段文件下载完毕后，进行合并，还原成原始文件，读取到内存中，并移除合并后的文件
  - 分段下载的核心参数为 `http` 请求头中的 `Range`
# 使用方法
## 步骤一: 创建代理生产者，并交给 spring 管理
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
## 步骤二: 创建 cookie 存储器,并交给 Spring 管理（此操作可选）
`如果不需要存储 cookie 方法空实现就好了,一些网站会用 cookie 标识用户身份，或者限速等操作`
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
        return new ArrayList<>();
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
## 步骤三 配置限速器，交由 spring 管理
`限速器可以按需使用，每次执行http 请求后，都会调用本类执行限速动作，多数CDN会对同一个ip请求频率做限制，如果不需要，则无需实现`
```java
package net.jlxxw.http.spider.proxy;

import com.google.common.util.concurrent.RateLimiter;
import net.jlxxw.http.spider.base.AbstractRateLimiter;
import org.apache.http.HttpHost;
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

```
## 编写YML配置文件
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

### 单元测试，开始采集数据之路
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
# 如果你觉得有用，也可以给我点个赞
![谢谢老板](image/d04e005a8218a068232d27d79fa72ec.jpg)
