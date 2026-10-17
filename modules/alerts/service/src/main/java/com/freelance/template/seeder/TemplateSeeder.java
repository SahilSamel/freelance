package com.freelance.template.seeder;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.ConfigurationEngine;
import com.freelance.keys.alerts.message.AlertEmitConfigKeys;
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
                    : ConfigurationEngine.getEnum(AlertEmitConfigKeys.DEFAULT_ALERT_CHANNEL, AlertChannel.class, AlertChannel.WHATSAPP);

            if (existingOpt.isEmpty()) {
                UUID templateId = UUID.nameUUIDFromBytes(("tpl:" + tpl.getTemplateKey()).getBytes());
                TemplateStatus initialStatus = tpl.getInitialStatus() != null
                        ? tpl.getInitialStatus()
                        : ((channel == AlertChannel.WHATSAPP && (tpl.getMetaTemplateId() == null || tpl.getMetaTemplateId().isBlank()))
                                ? TemplateStatus.DRAFT
                                : TemplateStatus.APPROVED);

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
                        initialStatus,
                        true,
                        attrs,
                        tpl.getMetaTemplateId(),
                        tpl.getPriority(),
                        tpl.getDispatchScope()
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
                    if (tpl.getInitialStatus() != null && entity.getStatus() != tpl.getInitialStatus()) {
                        entity.setStatus(tpl.getInitialStatus());
                        changed = true;
                    }
                    if (tpl.getDispatchScope() != null && entity.getDispatchScope() != tpl.getDispatchScope()) {
                        entity.setDispatchScope(tpl.getDispatchScope());
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

        // Pass 2: Link reply templates on button replies where replyTemplateKey is present
        for (ITemplate tpl : codeTemplates) {
            if (tpl.getButtons() == null || tpl.getButtons().isEmpty()) {
                continue;
            }
            Optional<AlertTemplateEntity> ownerTplOpt = templateRepository.findByKey(tpl.getTemplateKey());
            if (ownerTplOpt.isEmpty()) {
                continue;
            }
            AlertTemplateEntity ownerTpl = ownerTplOpt.get();
            boolean updated = false;

            for (ITemplateButton btn : tpl.getButtons()) {
                if (btn.getReplyTemplateKey() != null && !btn.getReplyTemplateKey().isBlank()) {
                    Optional<AlertTemplateEntity> replyTplOpt = templateRepository.findByKey(btn.getReplyTemplateKey());
                    if (replyTplOpt.isPresent()) {
                        AlertTemplateEntity replyTpl = replyTplOpt.get();
                        for (AlertButtonEntity btnEntity : ownerTpl.getButtons()) {
                            if (Objects.equals(btnEntity.getPayload(), btn.getPayload()) && btnEntity.getReply() != null) {
                                if (btnEntity.getReply().getReplyTemplate() == null
                                        || !Objects.equals(btnEntity.getReply().getReplyTemplate().getId(), replyTpl.getId())) {
                                    btnEntity.getReply().setReplyTemplate(replyTpl);
                                    updated = true;
                                }
                            }
                        }
                    }
                }
            }

            if (updated) {
                templateRepository.merge(ownerTpl);
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