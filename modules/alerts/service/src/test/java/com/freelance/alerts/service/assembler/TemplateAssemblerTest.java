package com.freelance.alerts.service.assembler;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.TemplateDTO;
import com.freelance.alerts.dto.TemplateListResponseDTO;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.template.assembler.TemplateAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateAssemblerTest {

    private final TemplateAssembler assembler = new TemplateAssembler();

    @Test
    @DisplayName("toDTO returns null when entity is null")
    void toDTO_returnsNullWhenEntityIsNull() {
        assertNull(assembler.toDTO(null));
    }

    @Test
    @DisplayName("toDTO maps all fields, required attributes, and buttons with replies")
    void toDTO_mapsAllFieldsCorrectly() {
        UUID templateId = UUID.randomUUID();
        AlertTemplateEntity entity = new AlertTemplateEntity(
                templateId,
                "ORDER_CONFIRMED",
                "Order Confirmation",
                "UTILITY",
                "en_US",
                "Order {{ORDER.order_id}} confirmed",
                "https://example.com/img.png",
                "IMAGE",
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                "ORDER.order_id,USER.name",
                "meta_tpl_123"
        );

        AlertButtonEntity btn = new AlertButtonEntity(
                UUID.randomUUID(),
                entity,
                "View Details",
                ButtonType.QUICK_REPLY,
                "view_order_payload",
                1
        );
        AlertButtonReplyEntity reply = new AlertButtonReplyEntity(
                UUID.randomUUID(),
                btn,
                null,
                true
        );
        btn.setReply(reply);
        entity.getButtons().add(btn);

        TemplateDTO dto = assembler.toDTO(entity);

        assertNotNull(dto);
        assertEquals(templateId, dto.id());
        assertEquals("ORDER_CONFIRMED", dto.templateKey());
        assertEquals("Order Confirmation", dto.name());
        assertEquals("UTILITY", dto.category());
        assertNull(dto.priority());
        assertEquals("en_US", dto.language());
        assertEquals("Order {{ORDER.order_id}} confirmed", dto.content());
        assertEquals("https://example.com/img.png", dto.mediaUrl());
        assertEquals("IMAGE", dto.mediaType());
        assertEquals(AlertChannel.WHATSAPP, dto.channel());
        assertEquals(TemplateStatus.APPROVED, dto.status());
        assertTrue(dto.isSystem());

        assertNotNull(dto.requiredAttributes());
        assertTrue(dto.requiredAttributes().contains("ORDER.order_id"));
        assertTrue(dto.requiredAttributes().contains("USER.name"));

        assertNotNull(dto.buttons());
        assertEquals(1, dto.buttons().size());
        assertEquals("View Details", dto.buttons().get(0).text());
        assertEquals(ButtonType.QUICK_REPLY, dto.buttons().get(0).type());
        assertEquals("view_order_payload", dto.buttons().get(0).payload());
        assertTrue(dto.buttons().get(0).requiresHuman());
        assertEquals(1, dto.buttons().get(0).sequenceOrder());
    }

    @Test
    @DisplayName("toDTO handles empty buttons and null requiredAttributes")
    void toDTO_handlesEmptyButtonsAndNullAttributes() {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "SIMPLE_KEY",
                "Simple",
                "UTILITY",
                "en",
                "Simple text",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.DRAFT,
                false,
                null,
                null
        );

        TemplateDTO dto = assembler.toDTO(entity);

        assertNotNull(dto);
        assertTrue(dto.requiredAttributes().isEmpty());
        assertTrue(dto.buttons().isEmpty());
        assertNull(dto.priority());
    }

    @Test
    @DisplayName("toDTO maps explicit priority correctly")
    void toDTO_mapsExplicitPriority() {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "EMERGENCY_TPL",
                "Emergency",
                "UTILITY",
                "en",
                "Emergency notice",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null,
                "HIGH"
        );

        TemplateDTO dto = assembler.toDTO(entity);
        assertNotNull(dto);
        assertEquals("HIGH", dto.priority());
        assertEquals("UTILITY", dto.category());
    }

    @Test
    @DisplayName("toListResponse returns empty list when input is null or empty")
    void toListResponse_returnsEmptyListWhenInputNullOrEmpty() {
        TemplateListResponseDTO nullResp = assembler.toListResponse(null);
        assertNotNull(nullResp);
        assertTrue(nullResp.templates().isEmpty());

        TemplateListResponseDTO emptyResp = assembler.toListResponse(Collections.emptyList());
        assertNotNull(emptyResp);
        assertTrue(emptyResp.templates().isEmpty());
    }

    @Test
    @DisplayName("toListResponse maps entities to DTOs")
    void toListResponse_mapsEntitiesToDTOs() {
        AlertTemplateEntity entity1 = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_1", "Name 1", "UTILITY", "en", "Content 1",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null
        );
        AlertTemplateEntity entity2 = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_2", "Name 2", "UTILITY", "en", "Content 2",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, null, null
        );

        TemplateListResponseDTO resp = assembler.toListResponse(List.of(entity1, entity2));
        assertNotNull(resp);
        assertEquals(2, resp.templates().size());
        assertEquals("TPL_1", resp.templates().get(0).templateKey());
        assertEquals("TPL_2", resp.templates().get(1).templateKey());
    }
}