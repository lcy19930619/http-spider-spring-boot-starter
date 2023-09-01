package net.jlxxw.http.spider.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基础文件内容
 *
 * @author chunyang.leng
 * @date 2023-08-31 14:00
 */
public class FileInfo {
    private static final Logger logger = LoggerFactory.getLogger(FileInfo.class);
    /**
     * 临时缓存目录
     */
    private static final String CACHE_PATH = ".";
    /**
     * 文件名称
     */
    private final String fileName;

    /**
     * 数据长度
     */
    private final long length;

    /**
     * 小于2gb的文件，可以使用内存存储
     */
    private byte[] data;
    /**
     * 大于2gb以上的文件时候，进行分段下载，临时缓存文件路径
     */
    private List<String> cachedFiles = new ArrayList<>();

    /**
     * 是否为大文件
     */
    private final boolean bigFile;

    /**
     * 文件重定向 url
     */
    private String redictUrl;

    /**
     * 是否下载失败
     */
    private boolean fail;

    /**
     * 创建一个文件信息
     *
     * @param fileName 文件名称
     * @param length   文件长度
     */
    public FileInfo(String fileName, long length) {
        this.fileName = fileName;
        this.length = length;
        this.bigFile = false;
    }

    /**
     * 创建一个文件信息
     *
     * @param fileName  文件名称
     * @param length    文件长度
     * @param shareSize 缓存文件数量
     */
    public FileInfo(String fileName, long length, int shareSize) {
        this.fileName = fileName;
        this.length = length;
        this.bigFile = true;
        this.cachedFiles = new ArrayList<>(shareSize);
        for (int i = 0; i < shareSize; i++) {
            cachedFiles.add(null);
        }
    }

    public String getRedictUrl() {
        return redictUrl;
    }

    public void setRedictUrl(String redictUrl) {
        this.redictUrl = redictUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public long getLength() {
        return length;
    }

    public boolean isBigFile() {
        return bigFile;
    }

    public byte[] getData() throws IOException {
        if (fail) {
            throw new IllegalStateException("file download failed");
        }

        if (bigFile) {
            for (String file : cachedFiles) {
                if (StringUtils.isBlank(file)) {
                    throw new IllegalStateException("file download failed");
                }
            }
            return mergeReadFile(".");
        }
        return data;
    }

    /**
     * 存储小文件
     *
     * @param data 小文件数据
     */
    public void saveLittleFile(byte[] data) {
        if (this.data != null) {
            // 已经存储过数据了，禁止再次覆盖
            throw new IllegalStateException("数据已经被存储了，禁止覆盖");
        }
        // 数组装得下
        this.data = data;
    }

    /**
     * 存大文件
     *
     * @param data  文件数据
     * @param index 分段位置
     * @throws IOException
     */
    public void saveBigFile(int index, byte[] data) throws IOException {
        String path = CACHE_PATH + "/" + UUID.randomUUID().toString();
        saveBigFile(path, index, data);
    }

    /**
     * 存储大文件数据
     *
     * @param cacheFilePath 缓存文件路径
     * @param index         分段位置
     * @param data          数据内容
     */
    public void saveBigFile(String cacheFilePath, int index, byte[] data) throws IOException {
        cachedFiles.set(index, cacheFilePath);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);) {
            saveBigFile(cacheFilePath,index,byteArrayInputStream);
        }
    }


    /**
     * 存大文件
     *
     * @param data  文件数据
     * @param index 分段位置
     * @throws IOException
     */
    public void saveBigFile(int index, InputStream data) throws IOException {
        String path = CACHE_PATH + "/temp/" + UUID.randomUUID().toString();
        saveBigFile(path, index, data);
    }

    /**
     * 存储大文件数据
     *
     * @param cacheFilePath 缓存文件路径
     * @param index         分段位置
     * @param inputStream          数据内容
     */
    public void saveBigFile(String cacheFilePath, int index, InputStream inputStream) throws IOException {
        String filePath = cacheFilePath + "/temp.cache." + index;
        File file = new File(filePath);
        FileUtils.createParentDirectories(file);
        cachedFiles.set(index, filePath);
        Files.copy(inputStream, Paths.get(filePath));
    }

    /**
     * 合并文件读取
     *
     * @param mergeOutPath 合并输出路径，例如：/a/b/c/d.txt
     * @return 读取的文件数据
     * @throws FileNotFoundException 尚未创建输出文件
     */
    public byte[] mergeReadFile(String mergeOutPath) throws IOException {
        if (cachedFiles.size() == 0) {
            throw new IllegalStateException("尚未存储文件");
        }
        String path = mergeOutPath + "/" + fileName;
        File out = new File(path);
        if (!out.exists()) {
            FileUtils.createParentDirectories(out);
            out.createNewFile();
        }
        RandomAccessFile resultFile = new RandomAccessFile(path, "rw");
        logger.info("开始合并分段数据");
        // 合并
        for (String cacheFilePath : cachedFiles) {
            logger.info("开始合并分段数据文件:{}", cacheFilePath);
            RandomAccessFile tempFile = new RandomAccessFile(cacheFilePath, "rw");
            tempFile.getChannel().transferTo(0, tempFile.length(), resultFile.getChannel());
            tempFile.close();
            logger.info("合并分段数据文件完毕:{}", cacheFilePath);
            File file = new File(cacheFilePath);
            file.delete();
            logger.info("合并分段数据文件清理完毕");
        }
        resultFile.close();
        return FileUtils.readFileToByteArray(out);
    }

    public boolean isFail() {
        return fail;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }
}
