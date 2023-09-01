package net.jlxxw.http.spider;

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
    public void htmlTest() throws Exception {
        List<String> urls = new ArrayList<String>();
        urls.add("https://www.huaweicloud.com");
        urls.add("https://www.baidu.com/");
        urls.add("https://www.huaweicloud.com");
        urls.add("https://www.baidu.com/");
        for (String url : urls) {
                httpSpider.html(url,new HttpHeaders(),(html)->{
                    Assertions.assertNotNull(html, "获取的html数据，不应该为空");
                });
        }
    }


    @Test
    public void download() throws Exception {
        List<String> urls = new ArrayList<String>();
        urls.add("https://www.winrar.com.cn/download/winrar-x64-611scp.exe");
        urls.add("https://download.jetbrains.com.cn/idea/ideaIU-2023.2.1.tar.gz");

        for (String url : urls) {
            FileInfo info = httpSpider.downloadFile(url, new HttpHeaders());
            boolean fail = info.isFail();
            Assertions.assertFalse(fail, "下载不应该失败");

            String name = info.getFileName();
            Assertions.assertTrue(StringUtils.isNotBlank(name),"获取的数据名称不可能为空");
            long length = info.getLength();

            Assertions.assertTrue(length > 0,"文件长度不可能小于 0");

            byte[] data = info.getData();
            Assertions.assertTrue(data.length > 0,"文件长度不可能小于 0");

        }
    }

}
