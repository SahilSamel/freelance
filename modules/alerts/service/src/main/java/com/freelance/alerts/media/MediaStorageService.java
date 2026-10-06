package com.freelance.alerts.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.stream.Stream;

public class MediaStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MediaStorageService.class);
    private final Path rootDir;

    public MediaStorageService() {
        this(resolveRootDirectory());
    }

    public MediaStorageService(Path rootDir) {
        this.rootDir = rootDir;
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            logger.error("Failed to create root media directory: {}", this.rootDir, e);
        }
    }

    private static Path resolveRootDirectory() {
        String customPath = System.getProperty("freelance.storage.media.path");
        if (customPath != null && !customPath.isBlank()) {
            return Paths.get(customPath);
        }
        return Paths.get("storage", "media");
    }

    private String sanitize(String input, String fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public Path getTenantDir(String tenantId) {
        String safeTenant = sanitize(tenantId, "default");
        Path tenantPath = rootDir.resolve(safeTenant).normalize();
        if (!tenantPath.startsWith(rootDir.normalize())) {
            throw new IllegalArgumentException("Invalid tenant identifier: " + tenantId);
        }
        try {
            Files.createDirectories(tenantPath);
        } catch (IOException e) {
            logger.error("Failed to create tenant media directory: {}", tenantPath, e);
        }
        return tenantPath;
    }

    public Path save(String tenantId, String fileName, byte[] data) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("Media data cannot be null");
        }
        Path tenantDir = getTenantDir(tenantId);
        String safeFileName = sanitize(fileName, "media_" + System.currentTimeMillis());
        Path targetPath = tenantDir.resolve(safeFileName).normalize();
        if (!targetPath.startsWith(tenantDir)) {
            throw new IllegalArgumentException("Path traversal attempt detected in fileName: " + fileName);
        }
        Files.write(targetPath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logger.info("[MediaStorageService] Saved {} bytes to {}", data.length, targetPath);
        return targetPath;
    }

    public Optional<byte[]> load(String tenantId, String fileName) {
        Optional<Path> fileOpt = findFile(tenantId, fileName);
        if (fileOpt.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(fileOpt.get()));
        } catch (IOException e) {
            logger.error("[MediaStorageService] Failed to read media file {}", fileOpt.get(), e);
            return Optional.empty();
        }
    }

    public Optional<Path> findFile(String tenantId, String fileNameOrMediaId) {
        Path tenantDir = getTenantDir(tenantId);
        String safe = sanitize(fileNameOrMediaId, "");
        if (safe.isBlank()) {
            return Optional.empty();
        }

        // Direct exact match
        Path direct = tenantDir.resolve(safe).normalize();
        if (direct.startsWith(tenantDir) && Files.exists(direct) && !Files.isDirectory(direct)) {
            return Optional.of(direct);
        }

        // Prefix match (e.g. mediaId without extension matching mediaId.jpg)
        try (Stream<Path> stream = Files.list(tenantDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.equalsIgnoreCase(safe) || name.startsWith(safe + ".");
                    })
                    .findFirst();
        } catch (IOException e) {
            logger.error("[MediaStorageService] Error listing directory {}", tenantDir, e);
            return Optional.empty();
        }
    }

    public boolean exists(String tenantId, String fileNameOrMediaId) {
        return findFile(tenantId, fileNameOrMediaId).isPresent();
    }

    public static String getMimeType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    public static String getExtensionForMimeType(String mimeType) {
        if (mimeType == null) return ".bin";
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> ".bin";
        };
    }
}
