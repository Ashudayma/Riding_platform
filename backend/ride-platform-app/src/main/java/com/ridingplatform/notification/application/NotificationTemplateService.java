package com.ridingplatform.notification.application;

import com.ridingplatform.config.NotificationProperties;
import com.ridingplatform.notification.domain.NotificationTemplateContent;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationTemplateEntity;
import com.ridingplatform.notification.infrastructure.persistence.NotificationTemplateJpaRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateJpaRepository notificationTemplateJpaRepository;
    private final NotificationProperties notificationProperties;

    public NotificationTemplateService(
            NotificationTemplateJpaRepository notificationTemplateJpaRepository,
            NotificationProperties notificationProperties
    ) {
        this.notificationTemplateJpaRepository = notificationTemplateJpaRepository;
        this.notificationProperties = notificationProperties;
    }

    public NotificationTemplateContent render(String eventCode, NotificationChannel channel, String locale, Map<String, Object> model) {
        NotificationTemplateEntity template = notificationTemplateJpaRepository.findActiveTemplate(eventCode, channel, locale)
                .or(() -> notificationTemplateJpaRepository.findActiveTemplate(eventCode, channel, notificationProperties.defaultLocale()))
                .orElseThrow(() -> new NotificationNotFoundException("Notification template not found for event " + eventCode + " and channel " + channel));
        return new NotificationTemplateContent(
                eventCode,
                replaceTokens(template.getTitleTemplate(), model),
                replaceTokens(template.getBodyTemplate(), model)
        );
    }

    private String replaceTokens(String template, Map<String, Object> model) {
        if (template == null) {
            return null;
        }
        String rendered = template;
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }
}
