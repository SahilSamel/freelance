package com.freelance.alerts.media.validation;

import com.freelance.alerts.dto.MediaUploadRequestDTO;
import com.freelance.keys.alerts.media.MediaValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class MediaUploadValidation extends AbstractValidation {

    /**
     * 16 MB binary file corresponds to ~22.4 MB in base64 string format.
     * We allow up to 25 MB characters to guard the JVM against memory exhaustion prior to decoding.
     */
    public static final int MAX_BASE64_LENGTH = 25 * 1024 * 1024;

    public void validateRequest(MediaUploadRequestDTO request) {
        if (request == null) {
            raiseValidationError(MediaValidationErrors.INVALID_MEDIA_DATA, "dataBase64");
            validate();
            return;
        }

        if (request.dataBase64() == null || request.dataBase64().isBlank()) {
            raiseValidationError(MediaValidationErrors.INVALID_MEDIA_DATA, "dataBase64");
        } else if (request.dataBase64().length() > MAX_BASE64_LENGTH) {
            raiseValidationError(MediaValidationErrors.MEDIA_PAYLOAD_TOO_LARGE, "dataBase64");
        }

        validate();
    }
}
