package com.freelance.template.assembler;

import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateDTO;
import com.freelance.alerts.dto.TemplateListResponseDTO;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;

import java.util.*;

public class TemplateAssembler {

    public TemplateDTO toDTO(AlertTemplateEntity entity) {
        if (entity == null) {
            return null;
        }

        List<TemplateButtonDTO> btnDtos = new ArrayList<>();
        if (entity.getButtons() != null) {
            for (AlertButtonEntity b : entity.getButtons()) {
                boolean reqHuman = b.getReply() != null && b.getReply().isRequiresHumanAgent();
                String replyKey = (b.getReply() != null && b.getReply().getReplyTemplate() != null)
                        ? b.getReply().getReplyTemplate().getTemplateKey()
                        : null;
                btnDtos.add(new TemplateButtonDTO(
                        b.getButtonText(),
                        b.getButtonType(),
                        b.getPayload(),
                        replyKey,
                        reqHuman,
                        b.getSequenceOrder()
                ));
            }
        }

        Set<String> attrs = new HashSet<>();
        if (entity.getRequiredAttributes() != null && !entity.getRequiredAttributes().isBlank()) {
            attrs.addAll(Arrays.asList(entity.getRequiredAttributes().split(",")));
        }

        return new TemplateDTO(
                entity.getId(),
                entity.getTemplateKey(),
                entity.getName(),
                entity.getCategory(),
                entity.getPriority(),
                entity.getLanguage(),
                entity.getContent(),
                entity.getMediaUrl(),
                entity.getMediaType(),
                entity.getChannel(),
                entity.getStatus(),
                entity.isSystem(),
                attrs,
                btnDtos,
                entity.getDispatchScope()
        );
    }

    public TemplateListResponseDTO toListResponse(List<AlertTemplateEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new TemplateListResponseDTO(Collections.emptyList());
        }

        List<TemplateDTO> dtos = entities.stream()
                .map(this::toDTO)
                .toList();

        return new TemplateListResponseDTO(dtos);
    }
}