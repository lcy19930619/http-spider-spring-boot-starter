package net.jlxxw.http.spider.properties;

import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author chunyang.leng
 * @date 2023-09-01 14:11
 */
@ConfigurationProperties("spider.file")
@Configuration
public class FileProperties {
    /**
     * 并发下载时，分片缓存文件大小，默认 15mb
     */
    private int shareSize = 15 * 1024 * 1024;

    /**
     * 并发下载时，分片缓存文件临时存储路径
     */
    private String cacheFilePath = "./cache";

    /**
     * 大文件限制，超过此大小，启动并发下载
     */
    private int bigFileSize = shareSize;

    /**
     * 允许下载的文件类型，参考 media 标准定义
     */
    private Set<String> mediaTypes = new HashSet<>();

    public int getShareSize() {
        return shareSize;
    }

    public void setShareSize(int shareSize) {
        this.shareSize = shareSize;
    }

    public String getCacheFilePath() {
        return cacheFilePath;
    }

    public void setCacheFilePath(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
    }

    public int getBigFileSize() {
        return bigFileSize;
    }

    public void setBigFileSize(int bigFileSize) {
        this.bigFileSize = bigFileSize;
    }

    public Set<String> getMediaTypes() {
        return mediaTypes;
    }

    public void setMediaTypes(Set<String> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }
}
