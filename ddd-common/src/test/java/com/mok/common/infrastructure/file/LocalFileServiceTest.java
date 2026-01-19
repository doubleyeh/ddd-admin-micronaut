package com.mok.common.infrastructure.file;

import com.mok.common.application.exception.BizException;
import com.mok.common.infrastructure.config.FileStorageProperties;
import io.micronaut.http.multipart.CompletedFileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalFileServiceTest {

    private FileStorageProperties properties;
    private LocalFileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        properties = new FileStorageProperties();
        setField(properties, "path", tempDir.toString());
        setField(properties, "domain", "http://localhost:8080");
        setField(properties, "prefix", "/files");

        fileService = new LocalFileService(properties);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void upload_CompletedFileUpload_Success() throws IOException {
        CompletedFileUpload file = mock(CompletedFileUpload.class);
        when(file.getFilename()).thenReturn("test.txt");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

        String url = fileService.upload(file);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/files/"));
        assertTrue(url.endsWith(".txt"));

        // Verify file exists
        String relativePath = url.substring("http://localhost:8080/files/".length());
        Path filePath = Paths.get(tempDir.toString(), relativePath);
        assertTrue(Files.exists(filePath));
        assertEquals("test content", Files.readString(filePath));
    }

    @Test
    void upload_CompletedFileUpload_IOException_ThrowsBizException() throws IOException {
        CompletedFileUpload file = mock(CompletedFileUpload.class);
        when(file.getFilename()).thenReturn("test.txt");
        when(file.getInputStream()).thenThrow(new IOException("Test IO error"));

        BizException exception = assertThrows(BizException.class, () -> fileService.upload(file));
        assertEquals("文件上传失败", exception.getMessage());
    }

    @Test
    void upload_InputStream_Success() {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        String fileName = "test.txt";

        String url = fileService.upload(inputStream, fileName);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/files/"));
        assertTrue(url.endsWith(".txt"));

        // Verify file exists
        String relativePath = url.substring("http://localhost:8080/files/".length());
        Path filePath = Paths.get(tempDir.toString(), relativePath);
        assertTrue(Files.exists(filePath));
    }

    @Test
    void upload_InputStream_FileNameWithoutExtension() {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        String fileName = "test";

        String url = fileService.upload(inputStream, fileName);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/files/"));

        String relativePath = url.substring("http://localhost:8080/files/".length());
        Path filePath = Paths.get(tempDir.toString(), relativePath);
        assertTrue(Files.exists(filePath));
    }

    @Test
    void upload_InputStream_FileNameWithMultipleDots() {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        String fileName = "test.tar.gz";

        String url = fileService.upload(inputStream, fileName);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/files/"));
        assertTrue(url.endsWith(".gz"));

        // Verify file exists
        String relativePath = url.substring("http://localhost:8080/files/".length());
        Path filePath = Paths.get(tempDir.toString(), relativePath);
        assertTrue(Files.exists(filePath));
    }

    @Test
    void upload_InputStream_FileNameDotOnly() {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        String fileName = ".";

        String url = fileService.upload(inputStream, fileName);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/files/"));
        String relativePath = url.substring("http://localhost:8080/files/".length());
        Path filePath = Paths.get(tempDir.toString(), relativePath);
        assertTrue(Files.exists(filePath));
    }

    @Test
    void upload_InputStream_FileNameNull() {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        String fileName = null;

        String url = fileService.upload(inputStream, fileName);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/files/"));
        String relativePath = url.substring("http://localhost:8080/files/".length());
        Path filePath = Paths.get(tempDir.toString(), relativePath);
        assertTrue(Files.exists(filePath));
    }

    @Test
    void upload_InputStream_TargetDirAlreadyExists() throws IOException {
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = Paths.get(tempDir.toString(), dateDir);
        Files.createDirectories(targetDir);

        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        String fileName = "test.txt";

        String url = fileService.upload(inputStream, fileName);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8080/files/"));
        assertTrue(url.endsWith(".txt"));

        String relativePath = url.substring("http://localhost:8080/files/".length());
        Path filePath = Paths.get(tempDir.toString(), relativePath);
        assertTrue(Files.exists(filePath));
    }

    @Test
    void delete_FileNotExists() {
        String url = "http://localhost:8080/files/2023/01/01/nonexistent.txt";

        fileService.delete(url);
    }

    @Test
    void delete_FilesDeleteIfExistsIOException() {
        String url = "http://localhost:8080/files/2023/01/01/test.txt";

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenThrow(new IOException("Delete failed"));

            fileService.delete(url);
        }
    }

    @Test
    void upload_InputStream_CreateDirectoriesIOException_ThrowsBizException() {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.createDirectories(any(Path.class))).thenThrow(new IOException("Create dir failed"));
            filesMock.when(() -> Files.copy(any(InputStream.class), any(Path.class), any())).then(invocation -> {
                return 0L;
            });

            InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
            String fileName = "test.txt";

            BizException exception = assertThrows(BizException.class, () -> fileService.upload(inputStream, fileName));
            assertEquals("文件保存失败", exception.getMessage());
        }
    }



    @Test
    void delete_ValidUrl_DeletesFile() throws IOException {
        // First create a file
        Path testFile = tempDir.resolve("2023/01/01/test.txt");
        Files.createDirectories(testFile.getParent());
        Files.writeString(testFile, "content");

        String url = "http://localhost:8080/files/2023/01/01/test.txt";

        fileService.delete(url);

        assertFalse(Files.exists(testFile));
    }

    @Test
    void delete_InvalidUrl_DoesNothing() {
        String url = "http://other.com/files/test.txt";

        fileService.delete(url);

        // No exception, no action
    }

    @Test
    void delete_NullUrl_DoesNothing() {
        fileService.delete(null);

        // No exception
    }

    @Test
    void delete_EmptyUrl_DoesNothing() {
        fileService.delete("");

        // No exception
    }
}
