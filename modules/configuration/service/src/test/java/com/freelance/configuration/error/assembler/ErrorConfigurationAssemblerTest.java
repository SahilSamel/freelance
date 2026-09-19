package com.freelance.configuration.error.assembler;

import com.freelance.configuration.dto.error.ErrorConfigurationDTO;
import com.freelance.configuration.dto.error.list.ErrorConfigurationListResponseDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateResponseDTO;
import com.freelance.configuration.error.entity.ErrorConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorConfigurationAssembler Unit Tests")
class ErrorConfigurationAssemblerTest {

    private final ErrorConfigurationAssembler assembler = new ErrorConfigurationAssembler();

    @Test
    @DisplayName("toDTO converts entity properly and handles null")
    void testToDTO() {
        assertNull(assembler.toDTO(null));

        LocalDateTime now = LocalDateTime.now();
        ErrorConfiguration entity = new ErrorConfiguration("user.not.found", 404, "User not found", "Description");
        entity.setCreatedDate(now);
        entity.setLastUpdatedDate(now);

        ErrorConfigurationDTO dto = assembler.toDTO(entity);
        assertNotNull(dto);
        assertEquals("user.not.found", dto.errorId());
        assertEquals(404, dto.statusCode());
        assertEquals("User not found", dto.errorMessage());
        assertEquals("Description", dto.description());
        assertEquals(now, dto.createdDate());
        assertEquals(now, dto.lastUpdatedDate());
    }

    @Test
    @DisplayName("toUpdateResponseDTO converts entity properly and handles null")
    void testToUpdateResponseDTO() {
        assertNull(assembler.toUpdateResponseDTO(null));

        ErrorConfiguration entity = new ErrorConfiguration("auth.failed", 401, "Auth failed", "Desc");
        ErrorConfigurationUpdateResponseDTO dto = assembler.toUpdateResponseDTO(entity);
        assertNotNull(dto);
        assertEquals("auth.failed", dto.errorId());
        assertEquals(401, dto.statusCode());
        assertEquals("Auth failed", dto.errorMessage());
        assertEquals("Desc", dto.description());
    }

    @Test
    @DisplayName("toListResponseDTO handles null, empty, single segment, multi segment, null errorId and null entity in list")
    void testToListResponseDTO() {
        ErrorConfigurationListResponseDTO empty1 = assembler.toListResponseDTO(null);
        assertNotNull(empty1);
        assertEquals(0, empty1.totalCount());
        assertTrue(empty1.groups().isEmpty());

        ErrorConfigurationListResponseDTO empty2 = assembler.toListResponseDTO(Collections.emptyList());
        assertNotNull(empty2);
        assertEquals(0, empty2.totalCount());

        List<ErrorConfiguration> list = new ArrayList<>();
        list.add(null);
        list.add(new ErrorConfiguration("singleError", 400, "Single", "Desc"));
        list.add(new ErrorConfiguration("auth.token.expired", 401, "Expired", "Desc"));
        list.add(new ErrorConfiguration("auth.token.invalid", 401, "Invalid", "Desc"));
        list.add(new ErrorConfiguration("auth.user.locked", 403, "Locked", "Desc"));
        list.add(new ErrorConfiguration("", 500, "Empty ID", "Desc"));

        ErrorConfigurationListResponseDTO response = assembler.toListResponseDTO(list);
        assertNotNull(response);
        assertEquals(5, response.totalCount());
        assertFalse(response.groups().isEmpty());
    }
}
