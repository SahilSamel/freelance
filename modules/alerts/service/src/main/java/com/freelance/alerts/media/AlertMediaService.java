package com.freelance.alerts.media;

import com.freelance.alerts.dto.MediaDownloadResponseDTO;
import com.freelance.alerts.dto.MediaUploadRequestDTO;
import com.freelance.alerts.dto.MediaUploadResponseDTO;
import com.freelance.alerts.media.constraint.MediaReadConstraint;
import com.freelance.alerts.media.constraint.MediaUploadConstraint;
import com.freelance.alerts.media.validation.MediaReadValidation;
import com.freelance.alerts.media.validation.MediaUploadValidation;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.rest.app.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AlertMediaService {

    private static final Logger logger = LoggerFactory.getLogger(AlertMediaService.class);

    private final MediaStorageService storageService;
    private final WhatsAppMediaService whatsAppMediaService;
    private final AlertMessageRepository messageRepository;
    private final MediaUploadValidation uploadValidation;
    private final MediaUploadConstraint uploadConstraint;
    private final MediaReadValidation readValidation;
    private final MediaReadConstraint readConstraint;

    public AlertMediaService() {
        this(new MediaStorageService(), new WhatsAppMediaService(), new AlertMessageRepository());
    }

    public AlertMediaService(MediaStorageService storageService, WhatsAppMediaService whatsAppMediaService) {
        this(storageService, whatsAppMediaService, null);
    }

    public AlertMediaService(MediaStorageService storageService, WhatsAppMediaService whatsAppMediaService, AlertMessageRepository messageRepository) {
        this(storageService, whatsAppMediaService, new MediaUploadValidation(), new MediaUploadConstraint(), new MediaReadValidation(), new MediaReadConstraint(), messageRepository);
    }

    public AlertMediaService(MediaStorageService storageService,
                             WhatsAppMediaService whatsAppMediaService,
                             MediaUploadValidation uploadValidation,
                             MediaReadValidation readValidation,
                             MediaReadConstraint readConstraint) {
        this(storageService, whatsAppMediaService, uploadValidation, new MediaUploadConstraint(), readValidation, readConstraint, null);
    }

    public AlertMediaService(MediaStorageService storageService,
                             WhatsAppMediaService whatsAppMediaService,
                             MediaUploadValidation uploadValidation,
                             MediaUploadConstraint uploadConstraint,
                             MediaReadValidation readValidation,
                             MediaReadConstraint readConstraint) {
        this(storageService, whatsAppMediaService, uploadValidation, uploadConstraint, readValidation, readConstraint, null);
    }

    public AlertMediaService(MediaStorageService storageService,
                             WhatsAppMediaService whatsAppMediaService,
                             MediaUploadValidation uploadValidation,
                             MediaUploadConstraint uploadConstraint,
                             MediaReadValidation readValidation,
                             MediaReadConstraint readConstraint,
                             AlertMessageRepository messageRepository) {
        this.storageService = storageService;
        this.whatsAppMediaService = whatsAppMediaService;
        this.messageRepository = messageRepository;
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

            // 1. Upload to Meta WhatsApp Media API to obtain Media ID if configured
            String mediaId = null;
            try {
                WhatsAppMediaService.UploadedMedia uploaded = whatsAppMediaService.uploadMedia(fileName, mimeType, rawBytes);
                if (uploaded != null && uploaded.mediaId() != null) {
                    mediaId = uploaded.mediaId();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Media upload was interrupted", ie);
            } catch (Exception upstreamEx) {
                logger.warn("[AlertMediaService] WhatsApp Media API upload skipped/failed ({}). Generating local media ID.", upstreamEx.getMessage());
            }

            if (mediaId == null || mediaId.isBlank()) {
                mediaId = "med_" + UUID.randomUUID().toString().replace("-", "");
            }

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
            String rawFileName = cachedPath.getFileName().toString();
            String mime = MediaStorageService.getMimeType(rawFileName);
            String friendlyFileName = resolveFriendlyFileName(mediaId, rawFileName);
            return Optional.of(new MediaDownloadResponseDTO(data, mime, friendlyFileName));
        } catch (IOException e) {
            logger.error("Cached media file '{}' exists but could not be read (corruption or lock)", cachedPath, e);
            throw new RuntimeException("Cached media file is unreadable: " + cachedPath.getFileName(), e);
        }
    }

    private String resolveFriendlyFileName(String mediaId, String defaultFileName) {
        try {
            if (messageRepository != null && mediaId != null && !mediaId.isBlank()) {
                Optional<AlertMessageEntity> msgOpt = messageRepository.findByMediaId(mediaId);
                if (msgOpt.isPresent()) {
                    AlertMessageEntity msg = msgOpt.get();
                    String content = msg.getContent() != null ? msg.getContent().toLowerCase() : "";
                    if (content.contains("receipt") || content.contains("payment")) {
                        String firstName = "Member";
                        try {
                            com.freelance.user.read.UserReadService readService = new com.freelance.user.read.UserReadService();
                            List<com.freelance.user.UserDTO> users = readService.readUsersByIds(List.of(msg.getUserId()));
                            if (users != null && !users.isEmpty() && users.get(0) != null) {
                                String fn = users.get(0).firstName();
                                if (fn != null && !fn.isBlank()) {
                                    firstName = fn.trim();
                                }
                            }
                        } catch (Exception ignored) {}
                        String dateStr = msg.getTimestamp() != null
                                ? msg.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
                                : java.time.LocalDate.now().toString();
                        return "Payment_Receipt_" + firstName + "_" + dateStr + ".pdf";
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[AlertMediaService] Failed to resolve friendly filename for mediaId={}: {}", mediaId, e.getMessage());
        }
        return defaultFileName;
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
