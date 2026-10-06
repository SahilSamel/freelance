package com.freelance.alerts.media.validation;

import com.freelance.keys.alerts.media.MediaValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class MediaReadValidation extends AbstractValidation {

    public void validateRequest(String mediaId) {
        if (mediaId == null || mediaId.isBlank()) {
            raiseValidationError(MediaValidationErrors.INVALID_MEDIA_ID, "mediaId");
        }

        validate();
    }
}
