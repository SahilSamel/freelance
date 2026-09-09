package com.freelance.configuration.error.assembler;

import com.freelance.configuration.dto.error.ErrorConfigurationDTO;
import com.freelance.configuration.dto.error.list.ErrorConfigurationGroupDTO;
import com.freelance.configuration.dto.error.list.ErrorConfigurationListResponseDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateResponseDTO;
import com.freelance.configuration.error.entity.ErrorConfiguration;

import java.util.*;

public class ErrorConfigurationAssembler {

    private static class MutableGroupNode {
        String name;
        Map<String, MutableGroupNode> subGroups = new LinkedHashMap<>();
        List<ErrorConfigurationDTO> errorConfigurations = new ArrayList<>();

        MutableGroupNode(String name) {
            this.name = name;
        }

        ErrorConfigurationGroupDTO toImmutableDTO() {
            List<ErrorConfigurationGroupDTO> subGroupDTOs = subGroups.values().stream()
                    .map(MutableGroupNode::toImmutableDTO)
                    .toList();
            return new ErrorConfigurationGroupDTO(name, subGroupDTOs, Collections.unmodifiableList(errorConfigurations));
        }
    }

    public ErrorConfigurationDTO toDTO(ErrorConfiguration entity) {
        if (entity == null) {
            return null;
        }
        return new ErrorConfigurationDTO(
                entity.getErrorId(),
                entity.getStatusCode(),
                entity.getErrorMessage(),
                entity.getDescription(),
                entity.getCreatedDate(),
                entity.getLastUpdatedDate()
        );
    }

    public ErrorConfigurationListResponseDTO toListResponseDTO(List<ErrorConfiguration> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ErrorConfigurationListResponseDTO(Collections.emptyList(), 0);
        }

        Map<String, MutableGroupNode> rootGroups = new LinkedHashMap<>();
        int count = 0;

        for (ErrorConfiguration entity : entities) {
            if (entity == null) continue;
            count++;
            ErrorConfigurationDTO dto = toDTO(entity);
            String errorId = entity.getErrorId();
            String[] segments = (errorId == null || errorId.isBlank())
                    ? new String[]{"Default"}
                    : errorId.split("\\.");

            String[] groupSegments;
            if (segments.length > 1) {
                groupSegments = Arrays.copyOf(segments, segments.length - 1);
            } else {
                groupSegments = new String[]{"Default"};
            }

            Map<String, MutableGroupNode> currentLevel = rootGroups;
            MutableGroupNode targetNode = null;

            for (String segment : groupSegments) {
                String trimmed = segment.trim();
                targetNode = currentLevel.computeIfAbsent(trimmed, MutableGroupNode::new);
                currentLevel = targetNode.subGroups;
            }

            if (targetNode != null) {
                targetNode.errorConfigurations.add(dto);
            }
        }

        List<ErrorConfigurationGroupDTO> groupDTOs = rootGroups.values().stream()
                .map(MutableGroupNode::toImmutableDTO)
                .toList();

        return new ErrorConfigurationListResponseDTO(groupDTOs, count);
    }

    public ErrorConfigurationUpdateResponseDTO toUpdateResponseDTO(ErrorConfiguration entity) {
        if (entity == null) {
            return null;
        }
        return new ErrorConfigurationUpdateResponseDTO(
                entity.getErrorId(),
                entity.getStatusCode(),
                entity.getErrorMessage(),
                entity.getDescription()
        );
    }
}
