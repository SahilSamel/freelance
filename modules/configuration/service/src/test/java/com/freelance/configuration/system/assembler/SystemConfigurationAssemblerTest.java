package com.freelance.configuration.system.assembler;

import com.freelance.configuration.dto.system.SystemConfigurationDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationListResponseDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateResponseDTO;
import com.freelance.configuration.system.entity.SystemConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SystemConfigurationAssembler Unit Tests")
class SystemConfigurationAssemblerTest {

    private final SystemConfigurationAssembler assembler = new SystemConfigurationAssembler();

    @Test
    @DisplayName("toDTO converts entity properly and handles null")
    void testToDTO() {
        assertNull(assembler.toDTO(null));

        LocalDateTime now = LocalDateTime.now();
        SystemConfiguration entity = new SystemConfiguration("app.timeout.seconds", "30", "Timeout in seconds", "^[0-9]+$");
        entity.setCreatedDate(now);
        entity.setLastUpdatedDate(now);

        SystemConfigurationDTO dto = assembler.toDTO(entity);
        assertNotNull(dto);
        assertEquals("app.timeout.seconds", dto.configId());
        assertEquals("30", dto.configValue());
        assertEquals("Timeout in seconds", dto.description());
        assertEquals("^[0-9]+$", dto.validation());
        assertEquals(now, dto.createdDate());
        assertEquals(now, dto.lastUpdatedDate());
    }

    @Test
    @DisplayName("toUpdateResponseDTO converts entity properly and handles null")
    void testToUpdateResponseDTO() {
        assertNull(assembler.toUpdateResponseDTO(null));

        SystemConfiguration entity = new SystemConfiguration("auth.max.attempts", "5", "Max login attempts", "^[0-9]+$");
        SystemConfigurationUpdateResponseDTO dto = assembler.toUpdateResponseDTO(entity);
        assertNotNull(dto);
        assertEquals("auth.max.attempts", dto.configId());
        assertEquals("5", dto.configValue());
        assertEquals("Max login attempts", dto.description());
    }

    @Test
    @DisplayName("toListResponseDTO handles null, empty, single segment, multi segment, null configId and null entity in list")
    void testToListResponseDTO() {
        SystemConfigurationListResponseDTO empty1 = assembler.toListResponseDTO(null);
        assertNotNull(empty1);
        assertEquals(0, empty1.totalCount());
        assertTrue(empty1.groups().isEmpty());

        SystemConfigurationListResponseDTO empty2 = assembler.toListResponseDTO(Collections.emptyList());
        assertNotNull(empty2);
        assertEquals(0, empty2.totalCount());

        List<SystemConfiguration> list = new ArrayList<>();
        list.add(null);
        list.add(new SystemConfiguration("singleConfig", "value", "Desc", null));
        list.add(new SystemConfiguration("auth.session.ttl", "3600", "TTL", "^[0-9]+$"));
        list.add(new SystemConfiguration("auth.session.max", "100", "Max", "^[0-9]+$"));
        list.add(new SystemConfiguration("auth.lockout.duration", "15", "Duration", "^[0-9]+$"));
        list.add(new SystemConfiguration("", "val", "Empty ID", null));

        SystemConfigurationListResponseDTO response = assembler.toListResponseDTO(list);
        assertNotNull(response);
        assertEquals(5, response.totalCount());
        assertFalse(response.groups().isEmpty());
    }
}
