package com.freelance.template.seeder;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.ConfigurationEngine;
import com.freelance.keys.alerts.AlertConfigKeys;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.system.SystemHardcoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TemplateSeeder {

    private static final Logger logger = LoggerFactory.getLogger(TemplateSeeder.class);
    private final AlertTemplateRepository templateRepository;

    public TemplateSeeder() {
        this(new AlertTemplateRepository());
    }

    public TemplateSeeder(AlertTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public void seed(List<ITemplate> codeTemplates) {
        if (codeTemplates == null || codeTemplates.isEmpty()) {
            return;
        }

        UUID systemUserId = SystemHardcoding.SYSTEM_USER_ID.asUuid();

        for (ITemplate tpl : codeTemplates) {
            Optional<AlertTemplateEntity> existingOpt = templateRepository.findByKey(tpl.getTemplateKey());
            String attrs = tpl.getRequiredAttributes() != null ? String.join(",", tpl.getRequiredAttributes()) : null;
            AlertChannel channel = tpl.getChannel() != null
                    ? tpl.getChannel()
                    : ConfigurationEngine.getEnum(AlertConfigKeys.DEFAULT_ALERT_CHANNEL, AlertChannel.class, AlertChannel.WHATSAPP);

            if (existingOpt.isEmpty()) {
                UUID templateId = UUID.nameUUIDFromBytes(("tpl:" + tpl.getTemplateKey()).getBytes());
                AlertTemplateEntity entity = new AlertTemplateEntity(
                        templateId,
                        tpl.getTemplateKey(),
                        tpl.getName(),
                        tpl.getCategory(),
                        tpl.getLanguage(),
                        tpl.getContent(),
                        tpl.getMediaUrl(),
                        tpl.getMediaType(),
                        channel,
                        TemplateStatus.APPROVED,
                        true,
                        attrs,
                        tpl.getMetaTemplateId(),
                        tpl.getPriority()
                );

                if (tpl.getButtons() != null) {
                    for (ITemplateButton btn : tpl.getButtons()) {
                        UUID buttonId = UUID.randomUUID();
                        AlertButtonEntity btnEntity = new AlertButtonEntity(
                                buttonId,
                                entity,
                                btn.getText(),
                                btn.getType(),
                                btn.getPayload(),
                                btn.getSequenceOrder()
                        );

                        AlertButtonReplyEntity replyEntity = new AlertButtonReplyEntity(
                                UUID.randomUUID(),
                                btnEntity,
                                null,
                                btn.requiresHumanAgent()
                        );
                        btnEntity.setReply(replyEntity);
                        entity.getButtons().add(btnEntity);
                    }
                }

                templateRepository.persist(entity);
                logger.info("[TemplateSeeder] Seeded new system template: {}", tpl.getTemplateKey());
            } else {
                AlertTemplateEntity entity = existingOpt.get();

                boolean notModifiedByAnyUser = (entity.getLastUpdatedBy() == null || entity.getLastUpdatedBy().equals(systemUserId));

                if (notModifiedByAnyUser) {
                    boolean changed = false;

                    if (!Objects.equals(entity.getName(), tpl.getName())) {
                        entity.setName(tpl.getName());
                        changed = true;
                    }
                    if (!Objects.equals(entity.getCategory(), tpl.getCategory())) {
                        entity.setCategory(tpl.getCategory());
                        changed = true;
                    }
                    if (!Objects.equals(entity.getLanguage(), tpl.getLanguage())) {
                        entity.setLanguage(tpl.getLanguage());
                        changed = true;
                    }
                    if (!Objects.equals(entity.getContent(), tpl.getContent())) {
                        entity.setContent(tpl.getContent());
                        changed = true;
                    }
                    if (!Objects.equals(entity.getMediaUrl(), tpl.getMediaUrl())) {
                        entity.setMediaUrl(tpl.getMediaUrl());
                        changed = true;
                    }
                    if (!Objects.equals(entity.getMediaType(), tpl.getMediaType())) {
                        entity.setMediaType(tpl.getMediaType());
                        changed = true;
                    }
                    if (entity.getChannel() != channel) {
                        entity.setChannel(channel);
                        changed = true;
                    }
                    if (!Objects.equals(entity.getRequiredAttributes(), attrs)) {
                        entity.setRequiredAttributes(attrs);
                        changed = true;
                    }
                    if (tpl.getMetaTemplateId() != null && !Objects.equals(entity.getMetaTemplateId(), tpl.getMetaTemplateId())) {
                        entity.setMetaTemplateId(tpl.getMetaTemplateId());
                        changed = true;
                    }
                    if (!Objects.equals(entity.getPriority(), tpl.getPriority())) {
                        entity.setPriority(tpl.getPriority());
                        changed = true;
                    }

                    if (buttonsChanged(entity.getButtons(), tpl.getButtons())) {
                        entity.getButtons().clear();
                        if (tpl.getButtons() != null) {
                            for (ITemplateButton btn : tpl.getButtons()) {
                                UUID buttonId = UUID.randomUUID();
                                AlertButtonEntity btnEntity = new AlertButtonEntity(
                                        buttonId,
                                        entity,
                                        btn.getText(),
                                        btn.getType(),
                                        btn.getPayload(),
                                        btn.getSequenceOrder()
                                );

                                AlertButtonReplyEntity replyEntity = new AlertButtonReplyEntity(
                                        UUID.randomUUID(),
                                        btnEntity,
                                        null,
                                        btn.requiresHumanAgent()
                                );
                                btnEntity.setReply(replyEntity);
                                entity.getButtons().add(btnEntity);
                            }
                        }
                        changed = true;
                    }

                    if (changed) {
                        templateRepository.merge(entity);
                        logger.info("[TemplateSeeder] Synchronized changes for system template: {}", tpl.getTemplateKey());
                    }
                } else {
                    logger.debug("[TemplateSeeder] Skipping update for template '{}' - modified by custom user '{}'",
                            tpl.getTemplateKey(), entity.getLastUpdatedBy());
                }
            }
        }
    }

    private boolean buttonsChanged(List<AlertButtonEntity> existingButtons, List<ITemplateButton> codeButtons) {
        if (codeButtons == null || codeButtons.isEmpty()) {
            return existingButtons != null && !existingButtons.isEmpty();
        }
        if (existingButtons == null || existingButtons.size() != codeButtons.size()) {
            return true;
        }

        for (int i = 0; i < codeButtons.size(); i++) {
            ITemplateButton codeBtn = codeButtons.get(i);
            AlertButtonEntity entityBtn = existingButtons.get(i);

            if (!Objects.equals(entityBtn.getButtonText(), codeBtn.getText())) return true;
            if (entityBtn.getButtonType() != codeBtn.getType()) return true;
            if (!Objects.equals(entityBtn.getPayload(), codeBtn.getPayload())) return true;
            if (entityBtn.getSequenceOrder() != codeBtn.getSequenceOrder()) return true;

            boolean entityRequiresHuman = entityBtn.getReply() != null && entityBtn.getReply().isRequiresHumanAgent();
            if (entityRequiresHuman != codeBtn.requiresHumanAgent()) return true;
        }

        return false;
    }
}