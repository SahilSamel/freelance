package com.freelance.alerts.service;

import com.freelance.alerts.dto.MediaDownloadResponseDTO;
import com.freelance.alerts.dto.MediaUploadRequestDTO;
import com.freelance.alerts.dto.MediaUploadResponseDTO;
import com.freelance.alerts.media.AlertMediaService;
import com.freelance.alerts.media.MediaStorageService;
import com.freelance.alerts.media.WhatsAppMediaService;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AlertMediaServiceTest {

    @TempDir
    Path tempDir;

    private MediaStorageService storageService;
    private WhatsAppMediaService whatsAppMediaService;
    private AlertMediaService mediaService;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        storageService = new MediaStorageService(tempDir);
        whatsAppMediaService = new WhatsAppMediaService() {
            @Override
            public UploadedMedia uploadMedia(String fileName, String mimeType, byte[] data) {
                return new UploadedMedia("meta_media_live_123");
            }

            @Override
            public DownloadedMedia downloadMedia(String mediaId) {
                return new DownloadedMedia("downloaded live binary".getBytes(), "image/png", mediaId + ".png");
            }
        };
        mediaService = new AlertMediaService(storageService, whatsAppMediaService);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should upload media, persist locally, and return valid response DTO")
    void testUploadMedia() {
        String testData = "Hello Media World";
        String base64 = Base64.getEncoder().encodeToString(testData.getBytes());

        MediaUploadRequestDTO request = new MediaUploadRequestDTO(
                "test.png",
                "image/png",
                base64
        );

        MediaUploadResponseDTO response = mediaService.uploadMedia(request);

        assertNotNull(response);
        assertEquals("meta_media_live_123", response.mediaId());
        assertEquals("/alerts/media/meta_media_live_123", response.mediaUrl());
        assertEquals("test.png", response.fileName());
        assertEquals("image/png", response.mimeType());
        assertEquals(testData.getBytes().length, response.fileSize());

        // Verify locally cached
        Optional<Path> cached = storageService.findFile("default", response.mediaId());
        assertTrue(cached.isPresent());
    }

    @Test
    @DisplayName("Should retrieve media from local storage if cached")
    void testGetMediaFromCache() throws IOException {
        String testData = "Cached binary content";
        storageService.save("default", "cached_123.jpg", testData.getBytes());

        MediaDownloadResponseDTO result = mediaService.getMedia("cached_123");

        assertNotNull(result);
        assertArrayEquals(testData.getBytes(), result.data());
        assertEquals("image/jpeg", result.mimeType());
    }

    @Test
    @DisplayName("Should lazy download from WhatsApp service if not in local cache")
    void testGetMediaLazyDownload() {
        MediaDownloadResponseDTO result = mediaService.getMedia("123456789");

        assertNotNull(result);
        assertNotNull(result.data());
        assertArrayEquals("downloaded live binary".getBytes(), result.data());
        assertEquals("image/png", result.mimeType());

        // Verify it was lazily cached to storageService
        Optional<Path> cached = storageService.findFile("default", "123456789");
        assertTrue(cached.isPresent());
    }

    @Test
    @DisplayName("Should throw ValidationException when upload media payload is missing or invalid")
    void testUploadMediaValidation() {
        assertThrows(ValidationException.class, () -> mediaService.uploadMedia(null));
        assertThrows(ValidationException.class, () -> mediaService.uploadMedia(new MediaUploadRequestDTO("test.png", "image/png", null)));
        assertThrows(ValidationException.class, () -> mediaService.uploadMedia(new MediaUploadRequestDTO("test.png", "image/png", "   ")));
    }

    @Test
    @DisplayName("Should throw ValidationException when mediaId is null or blank in getMedia")
    void testGetMediaValidation() {
        assertThrows(ValidationException.class, () -> mediaService.getMedia(null));
        assertThrows(ValidationException.class, () -> mediaService.getMedia("   "));
    }

    @Test
    @DisplayName("Should throw ConstraintException when media is not found")
    void testGetMediaNotFoundConstraint() {
        // Create an AlertMediaService with a WhatsAppMediaService that returns null on download
        WhatsAppMediaService failingWhatsAppService = new WhatsAppMediaService() {
            @Override
            public DownloadedMedia downloadMedia(String mediaId) {
                return null;
            }
        };
        AlertMediaService customService = new AlertMediaService(storageService, failingWhatsAppService);

        assertThrows(ConstraintException.class, () -> customService.getMedia("non_existent_id"));
    }

    @Test
    @DisplayName("Should enforce file size guard on media upload constraint")
    void testUploadMediaFileSizeGuard() {
        com.freelance.alerts.media.constraint.MediaUploadConstraint constraint = new com.freelance.alerts.media.constraint.MediaUploadConstraint();
        assertThrows(ConstraintException.class, () -> constraint.checkMediaSize(17 * 1024 * 1024L));
        assertDoesNotThrow(() -> constraint.checkMediaSize(1024L));
    }

    @Test
    @DisplayName("Should preserve thread interruption contract when download is interrupted")
    void testGetMediaInterruptedThreadContract() {
        WhatsAppMediaService interruptedWhatsAppService = new WhatsAppMediaService() {
            @Override
            public DownloadedMedia downloadMedia(String mediaId) throws InterruptedException {
                throw new InterruptedException("Simulated interruption");
            }
        };
        AlertMediaService customService = new AlertMediaService(storageService, interruptedWhatsAppService);

        try {
            assertThrows(RuntimeException.class, () -> customService.getMedia("interrupted_id"));
            assertTrue(Thread.currentThread().isInterrupted(), "Thread interrupt flag must be preserved");
        } finally {
            Thread.interrupted(); // Clear interrupt flag
        }
    }

    @Test
    @DisplayName("Should throw RuntimeException when cached file exists but cannot be read, distinguishing from clean miss")
    void testGetMediaCachedFileUnreadable() {
        MediaStorageService unreadableStorage = new MediaStorageService(tempDir) {
            @Override
            public Optional<Path> findFile(String tenantId, String fileNameOrMediaId) {
                return Optional.of(tempDir.resolve("non_existent_dir").resolve("unreadable.jpg"));
            }
        };
        AlertMediaService customService = new AlertMediaService(unreadableStorage, whatsAppMediaService);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> customService.getMedia("corrupted_media"));
        assertTrue(ex.getMessage().contains("Cached media file is unreadable"));
    }
}
