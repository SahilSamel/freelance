package com.freelance.alerts.media.constraint;

import com.freelance.keys.alerts.media.MediaConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class MediaUploadConstraint extends AbstractConstraint {

    /**
     * WhatsApp Media API upper limit for media upload is 16 MB.
     */
    public static final long MAX_MEDIA_SIZE_BYTES = 16 * 1024 * 1024L; // 16 MB

    public void checkMediaSize(long byteSize) {
        if (byteSize > MAX_MEDIA_SIZE_BYTES) {
            raiseConstraintError(MediaConstraintErrors.MEDIA_SIZE_EXCEEDED);
        }
    }
}
