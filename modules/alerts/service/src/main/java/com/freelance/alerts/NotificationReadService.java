package com.freelance.alerts;

import com.freelance.alerts.dto.NotificationReadRequestDTO;
import com.freelance.alerts.validation.NotificationReadValidation;

public class NotificationReadService {

    public void markAsRead(NotificationReadRequestDTO request) {
        new NotificationReadValidation().validateRequest(request);
        AlertsEngine.getInstance().markNotificationsRead(request);
    }
}

