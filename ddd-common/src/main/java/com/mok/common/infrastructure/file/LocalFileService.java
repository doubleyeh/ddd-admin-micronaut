package com.mok.common.infrastructure.file;

import com.mok.common.application.exception.BizException;
import com.mok.common.infrastructure.config.FileStorageProperties;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class LocalFileService implements FileService {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public String upload(CompletedFileUpload file) {
        try {
            return upload(file.getInputStream(), file.getFilename());
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BizException("文件上传失败");
        }
    }

    @Override
    public String upload(InputStream inputStream, String fileName) {
        try {
            // 生成日期目录
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String relativePath = dateDir + "/" + UUID.randomUUID().toString().replace("-", "") + getExtension(fileName);

            // 确保目录存在
            Path targetDir = Paths.get(fileStorageProperties.getPath(), dateDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // 保存文件
            Path targetPath = Paths.get(fileStorageProperties.getPath(), relativePath);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 返回访问URL
            return fileStorageProperties.getDomain() + fileStorageProperties.getPrefix() + "/" + relativePath;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException("文件保存失败");
        }
    }

    @Override
    public void delete(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }

        try {
            // 从URL中解析出相对路径
            String prefix = fileStorageProperties.getDomain() + fileStorageProperties.getPrefix() + "/";
            if (url.startsWith(prefix)) {
                String relativePath = url.substring(prefix.length());
                Path filePath = Paths.get(fileStorageProperties.getPath(), relativePath);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.error("文件删除失败: {}", url, e);
        }
    }

    private String getExtension(String fileName) {
        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }
}
