package com.aaax.core.utils;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

public class ResourcesUtil {

    @SneakyThrows
    private static <T> T read(String filePath, ResourceLoader resourceLoader, Class<T> targetType) {
        String location = "classpath:".concat(filePath);
        Resource resource = resourceLoader.getResource(location);
        InputStream inputStream = resource.getInputStream();
        return JSONUtil.readValue(inputStream, targetType);
    }

    @SneakyThrows
    private static InputStream read(String filePath, ResourceLoader resourceLoader) {
        String location = "classpath:".concat(filePath);
        Resource resource = resourceLoader.getResource(location);
        return resource.getInputStream();
    }

    @SneakyThrows
    public static InputStream readCsv(String filePath, ResourceLoader resourceLoader) {
        String fileExtension = ".csv";
        if (!filePath.contains(fileExtension)) {
            throw new BizException(SystemResponse.SYS9999, "filePath not contains ".concat(fileExtension));
        }
        return read(filePath, resourceLoader);
    }

    @SneakyThrows
    public static InputStream readJson(String filePath, ResourceLoader resourceLoader) {
        String fileExtension = ".json";
        if (!filePath.contains(fileExtension)) {
            throw new BizException(SystemResponse.SYS9999, "filePath not contains ".concat(fileExtension));
        }
        return read(filePath, resourceLoader);
    }

    @SneakyThrows
    public static <T> T readCsv(String filePath, ResourceLoader resourceLoader, Class<T> targetType) {
        String fileExtension = ".csv";
        if (!filePath.contains(fileExtension)) {
            throw new BizException(SystemResponse.SYS9999, "filePath not contains ".concat(fileExtension));
        }
        return read(filePath, resourceLoader, targetType);
    }

    @SneakyThrows
    public static <T> T readJson(String filePath, ResourceLoader resourceLoader, Class<T> targetType) {
        String fileExtension = ".json";
        if (!filePath.contains(fileExtension)) {
            throw new BizException(SystemResponse.SYS9999, "filePath not contains ".concat(fileExtension));
        }
        return read(filePath, resourceLoader, targetType);
    }

    @SneakyThrows
    public static <T> T readSpecificFile(String filePath, ResourceLoader resourceLoader, Class<T> targetType, String fileExtension) {
        if (!filePath.contains(fileExtension)) {
            throw new BizException(SystemResponse.SYS9999, "filePath not contains ".concat(fileExtension));
        }
        return read(filePath, resourceLoader, targetType);
    }
}
