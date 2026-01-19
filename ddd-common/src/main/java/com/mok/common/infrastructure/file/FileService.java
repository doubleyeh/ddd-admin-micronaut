package com.mok.common.infrastructure.file;

import io.micronaut.http.multipart.CompletedFileUpload;

import java.io.InputStream;

public interface FileService {
    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件访问URL
     */
    String upload(CompletedFileUpload file);

    /**
     * 上传文件
     *
     * @param inputStream 文件流
     * @param fileName    文件名
     * @return 文件访问URL
     */
    String upload(InputStream inputStream, String fileName);

    /**
     * 删除文件
     *
     * @param url 文件URL
     */
    void delete(String url);
}
