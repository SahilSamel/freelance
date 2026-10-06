package com.freelance.alerts.service;

import com.freelance.alerts.media.MediaStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MediaStorageServiceTest {

    @TempDir
    Path tempDir;

    private MediaStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new MediaStorageService(tempDir);
    }

    @Test
    @DisplayName("Should save and load media file for specific tenant")
    void testSaveAndLoad() throws IOException {
        String tenantId = "ironfit";
        String fileName = "receipt_01.jpg";
        byte[] data = "test image binary content".getBytes();

        Path saved = storageService.save(tenantId, fileName, data);
        assertNotNull(saved);
        assertTrue(storageService.exists(tenantId, fileName));

        Optional<byte[]> loaded = storageService.load(tenantId, fileName);
        assertTrue(loaded.isPresent());
        assertArrayEquals(data, loaded.get());
    }

    @Test
    @DisplayName("Should maintain strict tenant directory isolation")
    void testTenantIsolation() throws IOException {
        String tenantA = "ironfit";
        String tenantB = "crossfit";
        String fileName = "invoice.pdf";
        byte[] dataA = "ironfit invoice".getBytes();

        storageService.save(tenantA, fileName, dataA);

        assertTrue(storageService.exists(tenantA, fileName));
        assertFalse(storageService.exists(tenantB, fileName));
        assertTrue(storageService.load(tenantB, fileName).isEmpty());
    }

    @Test
    @DisplayName("Should resolve file by prefix without extension")
    void testFindByPrefix() throws IOException {
        String tenantId = "ironfit";
        String mediaId = "wamid_media_12345";
        byte[] data = "binary photo data".getBytes();

        storageService.save(tenantId, mediaId + ".png", data);

        assertTrue(storageService.exists(tenantId, mediaId));
        Optional<byte[]> loaded = storageService.load(tenantId, mediaId);
        assertTrue(loaded.isPresent());
        assertArrayEquals(data, loaded.get());
    }

    @Test
    @DisplayName("Should correctly identify MIME types and extensions")
    void testMimeTypesAndExtensions() {
        assertEquals("image/jpeg", MediaStorageService.getMimeType("photo.jpg"));
        assertEquals("image/jpeg", MediaStorageService.getMimeType("photo.jpeg"));
        assertEquals("image/png", MediaStorageService.getMimeType("photo.png"));
        assertEquals("image/webp", MediaStorageService.getMimeType("photo.webp"));
        assertEquals("application/pdf", MediaStorageService.getMimeType("doc.pdf"));
        assertEquals("application/octet-stream", MediaStorageService.getMimeType("unknown.xyz"));

        assertEquals(".jpg", MediaStorageService.getExtensionForMimeType("image/jpeg"));
        assertEquals(".png", MediaStorageService.getExtensionForMimeType("image/png"));
        assertEquals(".pdf", MediaStorageService.getExtensionForMimeType("application/pdf"));
    }
}
