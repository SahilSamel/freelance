package com.freelance.alerts.media;

import com.freelance.alerts.dto.MediaDownloadResponseDTO;
import com.freelance.alerts.dto.MediaUploadRequestDTO;
import com.freelance.alerts.dto.MediaUploadResponseDTO;
import com.freelance.alerts.media.constraint.MediaReadConstraint;
import com.freelance.alerts.media.constraint.MediaUploadConstraint;
import com.freelance.alerts.media.validation.MediaReadValidation;
import com.freelance.alerts.media.validation.MediaUploadValidation;
import com.freelance.rest.app.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class AlertMediaService {

    private static final Logger logger = LoggerFactory.getLogger(AlertMediaService.class);

    private final MediaStorageService storageService;
    private final WhatsAppMediaService whatsAppMediaService;
    private final MediaUploadValidation uploadValidation;
    private final MediaUploadConstraint uploadConstraint;
    private final MediaReadValidation readValidation;
    private final MediaReadConstraint readConstraint;

    public AlertMediaService() {
        this(new MediaStorageService(), new WhatsAppMediaService());
    }

    public AlertMediaService(MediaStorageService storageService, WhatsAppMediaService whatsAppMediaService) {
        this(storageService, whatsAppMediaService, new MediaUploadValidation(), new MediaUploadConstraint(), new MediaReadValidation(), new MediaReadConstraint());
    }

    public AlertMediaService(MediaStorageService storageService,
                             WhatsAppMediaService whatsAppMediaService,
                             MediaUploadValidation uploadValidation,
                             MediaReadValidation readValidation,
                             MediaReadConstraint readConstraint) {
        this(storageService, whatsAppMediaService, uploadValidation, new MediaUploadConstraint(), readValidation, readConstraint);
    }

    public AlertMediaService(MediaStorageService storageService,
                             WhatsAppMediaService whatsAppMediaService,
                             MediaUploadValidation uploadValidation,
                             MediaUploadConstraint uploadConstraint,
                             MediaReadValidation readValidation,
                             MediaReadConstraint readConstraint) {
        this.storageService = storageService;
        this.whatsAppMediaService = whatsAppMediaService;
        this.uploadValidation = uploadValidation;
        this.uploadConstraint = uploadConstraint;
        this.readValidation = readValidation;
        this.readConstraint = readConstraint;
    }

    public String resolveTenantId() {
        if (Transaction.hasActiveTransaction()
                && Transaction.getRequestContext() != null
                && Transaction.getRequestContext().tenantId() != null) {
            String tid = Transaction.getRequestContext().tenantId().trim();
            if (!tid.isBlank()) {
                return tid;
            }
        }
        return "default";
    }

    /**
     * Decodes inbound Base64 media data, uploads to WhatsApp Media API, and persists locally.
     * Enforces file size guard against WhatsApp 16 MB limit.
     *
     * @param request the upload request payload
     * @return the media upload response DTO with generated/assigned media ID and relative URL
     */
    public MediaUploadResponseDTO uploadMedia(MediaUploadRequestDTO request) {
        uploadValidation.validateRequest(request);

        try {
            String base64Data = request.dataBase64();
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            byte[] rawBytes = Base64.getDecoder().decode(base64Data);

            // Enforce file size guard after Base64 decoding
            uploadConstraint.checkMediaSize(rawBytes.length);

            String mimeType = request.mimeType() != null && !request.mimeType().isBlank()
                    ? request.mimeType()
                    : MediaStorageService.getMimeType(request.fileName());

            String ext = MediaStorageService.getExtensionForMimeType(mimeType);
            String fileName = (request.fileName() != null && !request.fileName().isBlank())
                    ? request.fileName()
                    : ("media_" + UUID.randomUUID().toString() + ext);

            String tenantId = resolveTenantId();

            // 1. Upload to Meta WhatsApp Media API to obtain Media ID
            WhatsAppMediaService.UploadedMedia uploaded = whatsAppMediaService.uploadMedia(fileName, mimeType, rawBytes);
            String mediaId = uploaded.mediaId();

            // 2. Persist to local storage under tenant partition with mediaId filename for fast retrieval
            String localFileName = mediaId + ext;
            storageService.save(tenantId, localFileName, rawBytes);

            return new MediaUploadResponseDTO(
                    mediaId,
                    "/alerts/media/" + mediaId,
                    fileName,
                    mimeType,
                    rawBytes.length
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Media upload was interrupted", e);
            throw new RuntimeException("Media upload was interrupted", e);
        } catch (IOException e) {
            logger.error("Failed to upload or store media", e);
            throw new RuntimeException("Failed to upload media: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves media binary content, checking local cache first and falling back to lazy download from Meta.
     * Refactored using Optional chaining without null sentinels, distinguishes corrupted cache from clean miss,
     * and preserves the thread interruption contract.
     *
     * @param mediaId the WhatsApp media ID
     * @return the downloaded media DTO containing bytes and mime type
     */
    public MediaDownloadResponseDTO getMedia(String mediaId) {
        readValidation.validateRequest(mediaId);

        String tenantId = resolveTenantId();

        // Idiomatic Optional chaining: cache hit -> fallback upstream download -> orElse null
        MediaDownloadResponseDTO result = loadFromCache(tenantId, mediaId)
                .or(() -> downloadFromUpstream(tenantId, mediaId))
                .orElse(null);

        // Enforce domain constraint: media must exist
        readConstraint.checkMediaExists(result);

        return result;
    }

    private Optional<MediaDownloadResponseDTO> loadFromCache(String tenantId, String mediaId) {
        Optional<java.nio.file.Path> cached = storageService.findFile(tenantId, mediaId);
        if (cached.isEmpty()) {
            return Optional.empty(); // Clean cache miss -> triggers upstream download fallback
        }

        java.nio.file.Path cachedPath = cached.get();
        try {
            byte[] data = Files.readAllBytes(cachedPath);
            String mime = MediaStorageService.getMimeType(cachedPath.getFileName().toString());
            return Optional.of(new MediaDownloadResponseDTO(data, mime, cachedPath.getFileName().toString()));
        } catch (IOException e) {
            logger.error("Cached media file '{}' exists but could not be read (corruption or lock)", cachedPath, e);
            throw new RuntimeException("Cached media file is unreadable: " + cachedPath.getFileName(), e);
        }
    }

    private Optional<MediaDownloadResponseDTO> downloadFromUpstream(String tenantId, String mediaId) {
        try {
            WhatsAppMediaService.DownloadedMedia downloaded = whatsAppMediaService.downloadMedia(mediaId);
            if (downloaded != null && downloaded.data() != null) {
                storageService.save(tenantId, downloaded.fileName(), downloaded.data());
                return Optional.of(new MediaDownloadResponseDTO(downloaded.data(), downloaded.mimeType(), downloaded.fileName()));
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Media download was interrupted for mediaId={}", mediaId, e);
            throw new RuntimeException("Media download was interrupted", e);
        } catch (Exception e) {
            logger.error("Failed to lazily download media from WhatsApp for mediaId={}", mediaId, e);
            return Optional.empty();
        }
    }
}
