package com.freelance.alerts.media.constraint;

import com.freelance.alerts.dto.MediaDownloadResponseDTO;
import com.freelance.keys.alerts.media.MediaConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class MediaReadConstraint extends AbstractConstraint {

    public void checkMediaExists(MediaDownloadResponseDTO media) {
        if (media == null) {
            raiseConstraintError(MediaConstraintErrors.MEDIA_NOT_FOUND);
        }
    }
}
