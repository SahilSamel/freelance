package com.freelance.template;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateCreateRequestDTO;
import com.freelance.alerts.dto.TemplateDTO;
import com.freelance.alerts.dto.TemplateListRequestDTO;
import com.freelance.alerts.dto.TemplateListResponseDTO;
import com.freelance.alerts.dto.TemplateCategoryDTO;
import com.freelance.alerts.dto.TemplateCategoryListResponseDTO;
import com.freelance.alerts.dto.TemplateSubmitResponseDTO;
import com.freelance.alerts.dto.TemplateBatchSubmitResponseDTO;
import com.freelance.alerts.dto.TemplateUpdateRequestDTO;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.ConfigurationEngine;
import com.freelance.keys.alerts.message.AlertEmitConfigKeys;
import com.freelance.template.assembler.TemplateAssembler;
import com.freelance.template.constraint.TemplateCreateConstraint;
import com.freelance.template.constraint.TemplateUpdateConstraint;
import com.freelance.template.validation.TemplateCreateValidation;
import com.freelance.template.validation.TemplateUpdateValidation;

import com.freelance.alerts.template.entity.AlertTemplateCategoryEntity;
import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppTemplateService;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppTemplateService.TemplateSubmissionResult;
import com.freelance.keys.alerts.template.TemplateConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;

import java.util.*;

public class TemplateService {

    private final AlertTemplateRepository templateRepository;
    private final AlertButtonRepository buttonRepository;
    private final AlertTemplateCategoryRepository categoryRepository;
    private final TemplateAssembler templateAssembler;
    private final WhatsAppTemplateService whatsAppTemplateService;

    public TemplateService() {
        this(new AlertTemplateRepository(), new AlertButtonRepository(), new AlertTemplateCategoryRepository(), new TemplateAssembler(), new WhatsAppTemplateService());
    }

    public TemplateService(AlertTemplateRepository templateRepository, AlertButtonRepository buttonRepository) {
        this(templateRepository, buttonRepository, new AlertTemplateCategoryRepository(), new TemplateAssembler(), new WhatsAppTemplateService());
    }

    public TemplateService(AlertTemplateRepository templateRepository, AlertButtonRepository buttonRepository, TemplateAssembler templateAssembler) {
        this(templateRepository, buttonRepository, new AlertTemplateCategoryRepository(), templateAssembler, new WhatsAppTemplateService());
    }

    public TemplateService(AlertTemplateRepository templateRepository, AlertButtonRepository buttonRepository,
                           AlertTemplateCategoryRepository categoryRepository, TemplateAssembler templateAssembler) {
        this(templateRepository, buttonRepository, categoryRepository, templateAssembler, new WhatsAppTemplateService());
    }

    public TemplateService(AlertTemplateRepository templateRepository, AlertButtonRepository buttonRepository,
                           AlertTemplateCategoryRepository categoryRepository, TemplateAssembler templateAssembler,
                           WhatsAppTemplateService whatsAppTemplateService) {
        this.templateRepository = templateRepository;
        this.buttonRepository = buttonRepository;
        this.categoryRepository = categoryRepository;
        this.templateAssembler = templateAssembler;
        this.whatsAppTemplateService = whatsAppTemplateService != null ? whatsAppTemplateService : new WhatsAppTemplateService();
    }

    public TemplateDTO createTemplate(TemplateCreateRequestDTO request) {
        new TemplateCreateValidation().validateRequest(request);
        new TemplateCreateConstraint(templateRepository, buttonRepository, categoryRepository).checkConstraints(request);

        List<TemplateCreateRequestDTO> allTemplates = request.allTemplates();
        Map<String, AlertTemplateEntity> templateEntities = new LinkedHashMap<>();

        for (TemplateCreateRequestDTO tplReq : allTemplates) {
            UUID templateId = UUID.randomUUID();
            String attrs = tplReq.requiredAttributes() != null ? String.join(",", tplReq.requiredAttributes()) : null;

            AlertChannel channel = tplReq.channel() != null
                    ? tplReq.channel()
                    : ConfigurationEngine.getEnum(AlertEmitConfigKeys.DEFAULT_ALERT_CHANNEL, AlertChannel.class, AlertChannel.WHATSAPP);
            TemplateStatus initialStatus = (channel == AlertChannel.PUSH) ? TemplateStatus.APPROVED : TemplateStatus.SUBMITTED;
            String priority = tplReq.priority() != null && !tplReq.priority().isBlank()
                    ? tplReq.priority().trim().toUpperCase()
                    : null;

            String category = (channel == AlertChannel.WHATSAPP && tplReq.category() != null && !tplReq.category().isBlank())
                    ? TemplateCategory.fromString(tplReq.category()).name()
                    : TemplateCategory.UTILITY.name();

            AlertTemplateEntity entity = new AlertTemplateEntity(
                    templateId,
                    tplReq.templateKey(),
                    tplReq.name() != null ? tplReq.name() : tplReq.templateKey(),
                    category,
                    tplReq.language() != null ? tplReq.language() : "en",
                    tplReq.content(),
                    tplReq.mediaUrl(),
                    tplReq.mediaType(),
                    channel,
                    initialStatus,
                    false,
                    attrs,
                    null,
                    priority
            );
            templateEntities.put(tplReq.templateKey(), entity);
        }

        for (TemplateCreateRequestDTO tplReq : allTemplates) {
            AlertTemplateEntity entity = templateEntities.get(tplReq.templateKey());
            if (tplReq.buttons() != null && entity.getChannel() == AlertChannel.WHATSAPP) {
                for (TemplateButtonDTO bDto : tplReq.buttons()) {
                    UUID buttonId = UUID.randomUUID();
                    AlertButtonEntity button = new AlertButtonEntity(
                            buttonId,
                            entity,
                            bDto.text(),
                            bDto.type(),
                            bDto.payload() != null ? bDto.payload() : buttonId.toString(),
                            bDto.sequenceOrder()
                    );

                    AlertTemplateEntity replyTemplate = null;
                    if (bDto.replyTemplateKey() != null) {
                        replyTemplate = templateEntities.get(bDto.replyTemplateKey());
                        if (replyTemplate == null) {
                            replyTemplate = templateRepository.findByKey(bDto.replyTemplateKey()).orElse(null);
                        }
                    }

                    AlertButtonReplyEntity reply = new AlertButtonReplyEntity(
                            UUID.randomUUID(),
                            button,
                            replyTemplate,
                            bDto.requiresHuman()
                    );
                    button.setReply(reply);
                    entity.getButtons().add(button);
                }
            }
        }

        // Submit WhatsApp templates to Meta Graph API
        for (TemplateCreateRequestDTO tplReq : allTemplates) {
            AlertTemplateEntity entity = templateEntities.get(tplReq.templateKey());
            if (entity.getChannel() == AlertChannel.WHATSAPP) {
                TemplateSubmissionResult result = whatsAppTemplateService.submitTemplate(entity, tplReq.buttons());
                if (!result.successful()) {
                    throw new ConstraintException(TemplateConstraintErrors.META_TEMPLATE_SUBMISSION_FAILED);
                }
                entity.setMetaTemplateId(result.metaTemplateId());
                entity.setStatus(result.status());
            }
        }

        // Persist all templates
        for (AlertTemplateEntity entity : templateEntities.values()) {
            templateRepository.persist(entity);
        }

        AlertTemplateEntity primaryEntity = (request.templateKey() != null && templateEntities.containsKey(request.templateKey()))
                ? templateEntities.get(request.templateKey())
                : templateEntities.values().iterator().next();

        return templateAssembler.toDTO(primaryEntity);
    }

    public TemplateListResponseDTO listTemplates(TemplateListRequestDTO request) {
        if (request != null && Boolean.TRUE.equals(request.all())) {
            List<AlertTemplateEntity> entities = templateRepository.findAllTemplates();
            if (request.category() != null && !request.category().isBlank()) {
                entities = entities.stream()
                        .filter(e -> request.category().equalsIgnoreCase(e.getCategory()))
                        .toList();
            }
            return templateAssembler.toListResponse(entities);
        }

        TemplateStatus targetStatus = (request != null && request.status() != null)
                ? request.status()
                : TemplateStatus.APPROVED;

        List<AlertTemplateEntity> entities = templateRepository.findByStatus(targetStatus);
        if (request != null && request.category() != null && !request.category().isBlank()) {
            entities = entities.stream()
                    .filter(e -> request.category().equalsIgnoreCase(e.getCategory()))
                    .toList();
        }

        return templateAssembler.toListResponse(entities);
    }

    public TemplateDTO getTemplate(UUID id) {
        AlertTemplateEntity entity = templateRepository.get(id);
        return templateAssembler.toDTO(entity);
    }

    public TemplateDTO updateTemplate(TemplateUpdateRequestDTO request) {
        new TemplateUpdateValidation().validateRequest(request);
        AlertTemplateEntity entity = new TemplateUpdateConstraint(templateRepository, buttonRepository, categoryRepository).checkConstraints(request);

        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.category() != null && !request.category().isBlank()) {
            entity.setCategory(request.category());
        }
        if (request.priority() != null) {
            entity.setPriority(request.priority().isBlank() ? null : request.priority().trim().toUpperCase());
        }
        if (request.language() != null && !request.language().isBlank()) {
            entity.setLanguage(request.language());
        }
        if (request.content() != null) {
            entity.setContent(request.content());
        }
        if (request.mediaUrl() != null) {
            entity.setMediaUrl(request.mediaUrl());
        }
        if (request.mediaType() != null) {
            entity.setMediaType(request.mediaType());
        }
        if (request.requiredAttributes() != null) {
            entity.setRequiredAttributes(String.join(",", request.requiredAttributes()));
        }

        if (request.buttons() != null) {
            entity.getButtons().clear();
            templateRepository.merge(entity);
            templateRepository.flush();

            for (TemplateButtonDTO bDto : request.buttons()) {
                UUID buttonId = UUID.randomUUID();
                AlertButtonEntity button = new AlertButtonEntity(
                        buttonId,
                        entity,
                        bDto.text(),
                        bDto.type(),
                        bDto.payload() != null ? bDto.payload() : buttonId.toString(),
                        bDto.sequenceOrder()
                );

                AlertTemplateEntity replyTemplate = null;
                if (bDto.replyTemplateKey() != null && !bDto.replyTemplateKey().isBlank()) {
                    replyTemplate = templateRepository.findByKey(bDto.replyTemplateKey().trim()).orElse(null);
                }

                AlertButtonReplyEntity reply = new AlertButtonReplyEntity(
                        UUID.randomUUID(),
                        button,
                        replyTemplate,
                        bDto.requiresHuman()
                );
                button.setReply(reply);
                entity.getButtons().add(button);
            }
        }

        if (entity.getChannel() == AlertChannel.WHATSAPP) {
            List<TemplateButtonDTO> btnDtos = request.buttons();
            if (btnDtos == null) {
                btnDtos = entity.getButtons().stream()
                        .map(b -> new TemplateButtonDTO(
                                b.getButtonText(),
                                b.getButtonType(),
                                b.getPayload(),
                                b.getReply() != null && b.getReply().getReplyTemplate() != null
                                        ? b.getReply().getReplyTemplate().getTemplateKey() : null,
                                b.getReply() != null && b.getReply().isRequiresHumanAgent(),
                                b.getSequenceOrder()
                        ))
                        .toList();
            }
            TemplateSubmissionResult result = whatsAppTemplateService.submitTemplate(entity, btnDtos);
            if (!result.successful()) {
                throw new ConstraintException(TemplateConstraintErrors.META_TEMPLATE_SUBMISSION_FAILED);
            }
            entity.setMetaTemplateId(result.metaTemplateId());
            entity.setStatus(result.status());
        }

        AlertTemplateEntity saved = templateRepository.merge(entity);
        return templateAssembler.toDTO(saved);
    }

    public TemplateCategoryListResponseDTO listCategories() {
        List<AlertTemplateCategoryEntity> categories = categoryRepository.findAllCategories();
        List<TemplateCategoryDTO> dtoList = categories.stream()
                .map(c -> new TemplateCategoryDTO(
                        c.getCategoryKey(),
                        c.getName(),
                        c.getDescription(),
                        c.getPriority()
                ))
                .toList();
        return new TemplateCategoryListResponseDTO(dtoList);
    }

    public TemplateSubmitResponseDTO submitTemplateForApproval(UUID id) {
        if (id == null) {
            return new TemplateSubmitResponseDTO(null, null, false, null, null, "Template ID cannot be null");
        }
        AlertTemplateEntity entity = templateRepository.get(id);
        if (entity == null) {
            return new TemplateSubmitResponseDTO(id, null, false, null, null, "Template not found: " + id);
        }
        if (entity.getChannel() != AlertChannel.WHATSAPP) {
            return new TemplateSubmitResponseDTO(
                    entity.getId(),
                    entity.getTemplateKey(),
                    false,
                    entity.getStatus(),
                    entity.getMetaTemplateId(),
                    "Only WHATSAPP channel templates can be submitted to Meta Graph API"
            );
        }
        return submitSingleEntity(entity);
    }

    public TemplateBatchSubmitResponseDTO submitAllPendingTemplates() {
        List<AlertTemplateEntity> submittable = templateRepository.findSubmittableWhatsAppTemplates();
        List<TemplateSubmitResponseDTO> results = new ArrayList<>();
        int submitted = 0;
        int failed = 0;

        for (AlertTemplateEntity entity : submittable) {
            TemplateSubmitResponseDTO result = submitSingleEntity(entity);
            results.add(result);
            if (result.successful()) {
                submitted++;
            } else {
                failed++;
            }
        }

        return new TemplateBatchSubmitResponseDTO(submittable.size(), submitted, failed, results);
    }

    private TemplateSubmitResponseDTO submitSingleEntity(AlertTemplateEntity entity) {
        List<TemplateButtonDTO> btnDtos = entity.getButtons().stream()
                .map(b -> new TemplateButtonDTO(
                        b.getButtonText(),
                        b.getButtonType(),
                        b.getPayload(),
                        b.getReply() != null && b.getReply().getReplyTemplate() != null
                                ? b.getReply().getReplyTemplate().getTemplateKey() : null,
                        b.getReply() != null && b.getReply().isRequiresHumanAgent(),
                        b.getSequenceOrder()
                ))
                .toList();

        TemplateSubmissionResult submissionResult = whatsAppTemplateService.submitTemplate(entity, btnDtos);
        if (submissionResult.successful()) {
            entity.setMetaTemplateId(submissionResult.metaTemplateId());
            entity.setStatus(submissionResult.status());
            templateRepository.merge(entity);
            return new TemplateSubmitResponseDTO(
                    entity.getId(),
                    entity.getTemplateKey(),
                    true,
                    entity.getStatus(),
                    entity.getMetaTemplateId(),
                    null
            );
        } else {
            return new TemplateSubmitResponseDTO(
                    entity.getId(),
                    entity.getTemplateKey(),
                    false,
                    entity.getStatus(),
                    entity.getMetaTemplateId(),
                    submissionResult.errorMessage()
            );
        }
    }
}